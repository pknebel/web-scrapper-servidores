package br.com.osb.web_scrapper_servidores.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.dto.response.BuscaAnualResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;
import br.com.osb.web_scrapper_servidores.dto.response.MesConsultadoDTO;
import br.com.osb.web_scrapper_servidores.dto.response.TotaisResponseDTO;

/**
 * Gera a planilha anual no mesmo formato do arquivo de referência.
 *
 * <h2>Layout (idêntico ao arquivo de referência)</h2>
 * <pre>
 *  linha 1  | QUADRO DE SERVIDORES DA PREFEITURA MUNICIPAL DE CHAPECO   (fonte 18, fundo azul)
 *  linha 2  | (em branco)
 *  linha 3  | ANO | DESCRIÇÃO | JANEIRO (2 col.) | FEVEREIRO (2 col.) | ... | DEZEMBRO (2 col.)
 *  linha 4  | ano | Servidores        | qtd | valor | qtd | valor | ...
 *  linhas 5-19 | demais categorias
 *  linha 20 | TOTAL DETALHADO NO PORTAL  =SOMA das linhas 5 a 19
 * </pre>
 *
 * <p>Cada mês ocupa duas colunas: quantidade e valor. Meses ainda não fechados ficam em branco,
 * como no arquivo de referência (que traz julho a dezembro vazios).</p>
 *
 * <p><b>Total:</b> a linha "TOTAL DETALHADO NO PORTAL" soma <b>todas</b> as categorias, inclusive
 * "Servidores". O arquivo de referência é ambíguo neste ponto — as fórmulas de janeiro a maio
 * ({@code =SUM(C5:C15)}) excluem "Servidores", mas o valor consolidado de junho (16.613) e as
 * fórmulas de julho a dezembro ({@code =SUM(Y4:Y13)}) o incluem, assim como o
 * {@code PlanilhaService} mensal já existente. Adotou-se a inclusão, coerente com o valor
 * efetivamente conferido no arquivo e com o comportamento atual do projeto.</p>
 *
 * <p>Para alternar o critério basta ajustar {@link #PRIMEIRA_LINHA_SOMADA}.</p>
 */
@Service
public class PlanilhaAnualService {

    private static final Logger log = LoggerFactory.getLogger(PlanilhaAnualService.class);

    private static final String NOME_ABA = "Quadro de Servidores";
    private static final String TITULO = "QUADRO DE SERVIDORES DA PREFEITURA MUNICIPAL DE CHAPECO";
    private static final String DESCRICAO_TOTAL = "TOTAL DETALHADO NO PORTAL";

    private static final String FORMATO_QUANTIDADE = "#,##0";
    private static final String FORMATO_MOEDA = "\"R$\"\\ #,##0.00;[Red]\\-\"R$\"\\ #,##0.00";

    /** Mesmo azul do arquivo de referência (B8CCE4), já utilizado no {@code PlanilhaService}. */
    private static final byte[] AZUL_CLARO = { (byte) 184, (byte) 204, (byte) 228 };

    private static final int LINHA_TITULO = 0;
    private static final int LINHA_CABECALHO = 2;
    private static final int PRIMEIRA_LINHA_CATEGORIA = 3;

    private static final int COLUNA_ANO = 0;
    private static final int COLUNA_DESCRICAO = 1;
    private static final int PRIMEIRA_COLUNA_MES = 2;
    private static final int MESES_NO_ANO = 12;
    private static final int COLUNAS_POR_MES = 2;
    private static final int TOTAL_COLUNAS = PRIMEIRA_COLUNA_MES + (MESES_NO_ANO * COLUNAS_POR_MES);

    private static final double LARGURA_ANO = 8.0;
    private static final double LARGURA_DESCRICAO = 24.13;
    private static final double LARGURA_QUANTIDADE = 12.0;
    private static final double LARGURA_VALOR = 14.75;

    private static final List<String> NOMES_DOS_MESES = List.of(
        "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
        "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
    );

    /**
     * Categorias na ordem exata do arquivo de referência.
     *
     * <p>As seis últimas não são fornecidas pelo serviço externo e ficam em branco, exatamente como
     * no arquivo de referência — mantê-las preserva a estrutura da planilha.</p>
     */
    private static final List<Categoria> CATEGORIAS = List.of(
        new Categoria("Servidores", BuscaDadosResponseDTO::servidores),
        new Categoria("Efetivos", BuscaDadosResponseDTO::efetivos),
        new Categoria("Celetistas", BuscaDadosResponseDTO::celetistas),
        new Categoria("Comissionados", BuscaDadosResponseDTO::comissionados),
        new Categoria("Aposentados", BuscaDadosResponseDTO::aposentados),
        new Categoria("Pensionistas", BuscaDadosResponseDTO::pensionistas),
        new Categoria("Estagiários", BuscaDadosResponseDTO::estagiarios),
        new Categoria("Cedidos/Recebidos", BuscaDadosResponseDTO::cedidosRecebidos),
        new Categoria("Temporários", BuscaDadosResponseDTO::temporarios),
        new Categoria("Agentes Políticos", BuscaDadosResponseDTO::agentePolitico),
        new Categoria("Emprego público", null),
        new Categoria("Conselho tutelar", null),
        new Categoria("Autônomo", null),
        new Categoria("Outros", null),
        new Categoria("Eletivo", null),
        new Categoria("Jovem Aprendiz", null)
    );

    private static final int LINHA_TOTAL = PRIMEIRA_LINHA_CATEGORIA + CATEGORIAS.size();

    /**
     * Primeira linha (1-based, referência do Excel) incluída na soma da linha de total.
     *
     * <p>{@code PRIMEIRA_LINHA_CATEGORIA + 1} corresponde à linha 4, ou seja, "Servidores" entra na
     * soma. Para excluí-la, some 2 em vez de 1.</p>
     */
    private static final int PRIMEIRA_LINHA_SOMADA = PRIMEIRA_LINHA_CATEGORIA + 1;

    /** Última linha (1-based) incluída na soma: a última categoria da planilha. */
    private static final int ULTIMA_LINHA_SOMADA = PRIMEIRA_LINHA_CATEGORIA + CATEGORIAS.size();

    public byte[] gerarPlanilha(BuscaAnualResponseDTO resultado) throws IOException {
        log.info("Gerando planilha anual de {} com {} mês(es) preenchido(s).",
                resultado.ano(), resultado.meses().size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(NOME_ABA);
            Estilos estilos = new Estilos(workbook);

            criarTitulo(sheet, estilos);
            criarCabecalho(sheet, estilos);
            criarCategorias(sheet, estilos, resultado);
            criarTotal(sheet, estilos);
            ajustarLarguras(sheet);

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void criarTitulo(Sheet sheet, Estilos estilos) {
        Row linha = sheet.createRow(LINHA_TITULO);

        for (int coluna = 0; coluna < TOTAL_COLUNAS; coluna++) {
            linha.createCell(coluna).setCellStyle(estilos.titulo());
        }

        linha.getCell(COLUNA_ANO).setCellValue(TITULO);
    }

    private void criarCabecalho(Sheet sheet, Estilos estilos) {
        Row linha = sheet.createRow(LINHA_CABECALHO);

        for (int coluna = 0; coluna < TOTAL_COLUNAS; coluna++) {
            linha.createCell(coluna).setCellStyle(estilos.cabecalho());
        }

        linha.getCell(COLUNA_ANO).setCellValue("ANO");
        linha.getCell(COLUNA_DESCRICAO).setCellValue("DESCRIÇÃO");

        for (int mes = 1; mes <= MESES_NO_ANO; mes++) {
            linha.getCell(colunaQuantidade(mes)).setCellValue(NOMES_DOS_MESES.get(mes - 1));
        }
    }

    private void criarCategorias(Sheet sheet, Estilos estilos, BuscaAnualResponseDTO resultado) {
        for (int indice = 0; indice < CATEGORIAS.size(); indice++) {
            Categoria categoria = CATEGORIAS.get(indice);
            Row linha = sheet.createRow(PRIMEIRA_LINHA_CATEGORIA + indice);

            criarCelulasVazias(linha, estilos);

            // O ano aparece uma única vez, na primeira linha de dados, como no arquivo de referência.
            if (indice == 0) {
                Cell celulaAno = linha.getCell(COLUNA_ANO);
                celulaAno.setCellValue(resultado.ano());
                celulaAno.setCellStyle(estilos.ano());
            }

            linha.getCell(COLUNA_DESCRICAO).setCellValue(categoria.descricao());

            if (categoria.possuiDados()) {
                preencherMeses(linha, estilos, resultado, categoria);
            }
        }
    }

    private void preencherMeses(Row linha, Estilos estilos, BuscaAnualResponseDTO resultado, Categoria categoria) {
        for (int mes = 1; mes <= MESES_NO_ANO; mes++) {
            Optional<MesConsultadoDTO> mesConsultado = resultado.mes(mes);

            if (mesConsultado.isEmpty()) {
                continue; // mês ainda não fechado ou que falhou: permanece em branco
            }

            TotaisResponseDTO totais = categoria.extrair(mesConsultado.get().dados());

            preencherNumero(linha, colunaQuantidade(mes), totais.QUANTIDADE(), estilos.quantidade());
            preencherNumero(linha, colunaValor(mes), totais.VALOR(), estilos.moeda());
        }
    }

    private void criarTotal(Sheet sheet, Estilos estilos) {
        Row linha = sheet.createRow(LINHA_TOTAL);

        for (int coluna = 0; coluna < TOTAL_COLUNAS; coluna++) {
            linha.createCell(coluna).setCellStyle(estilos.totalTexto());
        }

        linha.getCell(COLUNA_DESCRICAO).setCellValue(DESCRICAO_TOTAL);

        for (int mes = 1; mes <= MESES_NO_ANO; mes++) {
            aplicarSoma(linha, colunaQuantidade(mes), estilos.totalQuantidade());
            aplicarSoma(linha, colunaValor(mes), estilos.totalMoeda());
        }
    }

    /**
     * Escreve a fórmula de soma da coluna. As referências são 1-based por serem fórmulas do Excel.
     *
     * <p>Usar fórmula em vez de valor calculado mantém a planilha viva: se o usuário editar uma
     * célula, o total se ajusta — exatamente como no arquivo de referência.</p>
     */
    private void aplicarSoma(Row linha, int coluna, XSSFCellStyle estilo) {
        String letra = CellReference.convertNumToColString(coluna);

        Cell celula = linha.getCell(coluna);
        celula.setCellFormula(
            "SUM(%s%d:%s%d)".formatted(letra, PRIMEIRA_LINHA_SOMADA, letra, ULTIMA_LINHA_SOMADA));
        celula.setCellStyle(estilo);
    }

    private void criarCelulasVazias(Row linha, Estilos estilos) {
        for (int coluna = 0; coluna < TOTAL_COLUNAS; coluna++) {
            linha.createCell(coluna).setCellStyle(estilos.texto());
        }
    }

    private void preencherNumero(Row linha, int coluna, BigDecimal valor, XSSFCellStyle estilo) {
        Cell celula = linha.getCell(coluna);
        celula.setCellValue(valor == null ? 0d : valor.doubleValue());
        celula.setCellStyle(estilo);
    }

    private void ajustarLarguras(Sheet sheet) {
        sheet.setColumnWidth(COLUNA_ANO, largura(LARGURA_ANO));
        sheet.setColumnWidth(COLUNA_DESCRICAO, largura(LARGURA_DESCRICAO));

        for (int mes = 1; mes <= MESES_NO_ANO; mes++) {
            sheet.setColumnWidth(colunaQuantidade(mes), largura(LARGURA_QUANTIDADE));
            sheet.setColumnWidth(colunaValor(mes), largura(LARGURA_VALOR));
        }
    }

    private static int largura(double caracteres) {
        return (int) Math.round(caracteres * 256);
    }

    private static int colunaQuantidade(int mes) {
        return PRIMEIRA_COLUNA_MES + ((mes - 1) * COLUNAS_POR_MES);
    }

    private static int colunaValor(int mes) {
        return colunaQuantidade(mes) + 1;
    }

    /** Categoria da planilha e como extrair seus totais do resultado de um mês. */
    private record Categoria(String descricao, Function<BuscaDadosResponseDTO, TotaisResponseDTO> extrator) {

        boolean possuiDados() {
            return extrator != null;
        }

        TotaisResponseDTO extrair(BuscaDadosResponseDTO dados) {
            TotaisResponseDTO totais = extrator.apply(dados);
            return totais == null ? TotaisResponseDTO.zerado() : totais;
        }
    }

    /** Estilos criados uma única vez por planilha — o Excel limita o número de estilos por arquivo. */
    private static final class Estilos {

        private final XSSFCellStyle titulo;
        private final XSSFCellStyle cabecalho;
        private final XSSFCellStyle ano;
        private final XSSFCellStyle texto;
        private final XSSFCellStyle quantidade;
        private final XSSFCellStyle moeda;
        private final XSSFCellStyle totalTexto;
        private final XSSFCellStyle totalQuantidade;
        private final XSSFCellStyle totalMoeda;

        private Estilos(XSSFWorkbook workbook) {
            XSSFColor azul = new XSSFColor(AZUL_CLARO, new DefaultIndexedColorMap());
            short formatoQuantidade = workbook.createDataFormat().getFormat(FORMATO_QUANTIDADE);
            short formatoMoeda = workbook.createDataFormat().getFormat(FORMATO_MOEDA);

            XSSFFont fonteTitulo = workbook.createFont();
            fonteTitulo.setFontHeightInPoints((short) 18);

            XSSFFont fonteNegrito = workbook.createFont();
            fonteNegrito.setBold(true);

            this.titulo = novoEstilo(workbook, azul, false);
            this.titulo.setFont(fonteTitulo);

            this.cabecalho = novoEstilo(workbook, azul, true);

            this.ano = novoEstilo(workbook, null, true);
            this.ano.setFont(fonteNegrito);
            this.ano.setAlignment(HorizontalAlignment.CENTER);

            this.texto = novoEstilo(workbook, null, true);

            this.quantidade = novoEstilo(workbook, null, true);
            this.quantidade.setDataFormat(formatoQuantidade);
            this.quantidade.setAlignment(HorizontalAlignment.RIGHT);

            this.moeda = novoEstilo(workbook, null, true);
            this.moeda.setDataFormat(formatoMoeda);
            this.moeda.setAlignment(HorizontalAlignment.RIGHT);

            this.totalTexto = novoEstilo(workbook, azul, true);

            this.totalQuantidade = novoEstilo(workbook, azul, true);
            this.totalQuantidade.setDataFormat(formatoQuantidade);
            this.totalQuantidade.setAlignment(HorizontalAlignment.RIGHT);

            this.totalMoeda = novoEstilo(workbook, azul, true);
            this.totalMoeda.setDataFormat(formatoMoeda);
            this.totalMoeda.setAlignment(HorizontalAlignment.RIGHT);
        }

        private static XSSFCellStyle novoEstilo(XSSFWorkbook workbook, XSSFColor fundo, boolean comBorda) {
            XSSFCellStyle estilo = workbook.createCellStyle();

            if (fundo != null) {
                estilo.setFillForegroundColor(fundo);
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            if (comBorda) {
                estilo.setBorderTop(BorderStyle.THIN);
                estilo.setBorderBottom(BorderStyle.THIN);
                estilo.setBorderLeft(BorderStyle.THIN);
                estilo.setBorderRight(BorderStyle.THIN);
            }

            return estilo;
        }

        private XSSFCellStyle titulo() {
            return titulo;
        }

        private XSSFCellStyle cabecalho() {
            return cabecalho;
        }

        private XSSFCellStyle ano() {
            return ano;
        }

        private XSSFCellStyle texto() {
            return texto;
        }

        private XSSFCellStyle quantidade() {
            return quantidade;
        }

        private XSSFCellStyle moeda() {
            return moeda;
        }

        private XSSFCellStyle totalTexto() {
            return totalTexto;
        }

        private XSSFCellStyle totalQuantidade() {
            return totalQuantidade;
        }

        private XSSFCellStyle totalMoeda() {
            return totalMoeda;
        }
    }
}
