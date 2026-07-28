package br.com.osb.web_scrapper_servidores.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import br.com.osb.web_scrapper_servidores.dto.response.BuscaDadosResponseDTO;

@Service
public class PlanilhaService {

    private void preencherValor(Row row, int coluna, BigDecimal valor, CellStyle style) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(style);
    }

    private void preencherQuantidade(Row row, int coluna, BigDecimal valor) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor.doubleValue());
    }

    private void criarCelulaComCor(Row row, int coluna, Object valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);

        if (valor instanceof Number numero) {
            cell.setCellValue(numero.doubleValue());
        } else {
            cell.setCellValue(valor.toString());
        }

        cell.setCellStyle(estilo);
    }

    public byte[] gerarPlanilha(LocalDate data,BuscaDadosResponseDTO dados) throws IOException {

        int ano = data.getYear();
        String mes = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        mes = mes.toUpperCase();
        
        data.getMonthValue();

        BigDecimal quantidadeTotal =
        dados.servidores().QUANTIDADE()
            .add(dados.efetivos().QUANTIDADE())
            .add(dados.comissionados().QUANTIDADE())
            .add(dados.celetistas().QUANTIDADE())
            .add(dados.aposentados().QUANTIDADE())
            .add(dados.pensionistas().QUANTIDADE())
            .add(dados.estagiarios().QUANTIDADE())
            .add(dados.cedidosRecebidos().QUANTIDADE())
            .add(dados.temporarios().QUANTIDADE())
            .add(dados.agentePolitico().QUANTIDADE());

        BigDecimal valorTotal =
        dados.servidores().VALOR()
            .add(dados.efetivos().VALOR())
            .add(dados.comissionados().VALOR())
            .add(dados.celetistas().VALOR())
            .add(dados.aposentados().VALOR())
            .add(dados.pensionistas().VALOR())
            .add(dados.estagiarios().VALOR())
            .add(dados.cedidosRecebidos().VALOR())
            .add(dados.temporarios().VALOR())
            .add(dados.agentePolitico().VALOR());
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Quadro de Servidores");
        DataFormat format = workbook.createDataFormat();

        CellStyle moedaStyle = workbook.createCellStyle();
        moedaStyle.setDataFormat(format.getFormat("R$ #,##0.00"));

        XSSFCellStyle corFundo = (XSSFCellStyle) workbook.createCellStyle();

        byte[] azulClaro = {
            (byte) 184,
            (byte) 204,
            (byte) 228
        };

        corFundo.setFillForegroundColor(new XSSFColor(azulClaro, new DefaultIndexedColorMap()));
        corFundo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle moedaComCorStyle = workbook.createCellStyle();

        moedaComCorStyle.setFillForegroundColor(
            new XSSFColor(
                new byte[] {
                    (byte) 184,
                    (byte) 204,
                    (byte) 228
                },
                null
            )
        );

        moedaComCorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        moedaComCorStyle.setDataFormat(
            workbook.createDataFormat().getFormat("R$ #,##0.00")
        );

        Row header = sheet.createRow(0);
        criarCelulaComCor(header, 0, ano, corFundo);
        criarCelulaComCor(header, 1, "DESCRIÇÃO", corFundo);
        criarCelulaComCor(header, 2, mes, corFundo);
        criarCelulaComCor(header, 3, "TOTAL MENSAL", corFundo);

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("");
        row1.createCell(1).setCellValue("Servidores");
        preencherQuantidade(row1, 2, dados.servidores().QUANTIDADE());
        preencherValor(row1, 3, dados.servidores().VALOR(), moedaStyle);

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("");
        row2.createCell(1).setCellValue("Efetivos");
        preencherQuantidade(row2, 2, dados.efetivos().QUANTIDADE());
        preencherValor(row2, 3, dados.efetivos().VALOR(), moedaStyle);

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("");
        row3.createCell(1).setCellValue("Celetistas");
        preencherQuantidade(row3, 2, dados.celetistas().QUANTIDADE());
        preencherValor(row3, 3, dados.celetistas().VALOR(), moedaStyle);

        Row row4 = sheet.createRow(4);
        row4.createCell(0).setCellValue("");
        row4.createCell(1).setCellValue("Comissionados");
        preencherQuantidade(row4, 2, dados.comissionados().QUANTIDADE());
        preencherValor(row4, 3, dados.comissionados().VALOR(), moedaStyle);

        Row row5 = sheet.createRow(5);
        row5.createCell(0).setCellValue("");
        row5.createCell(1).setCellValue("Aposentados");
        preencherQuantidade(row5, 2, dados.aposentados().QUANTIDADE());
        preencherValor(row5, 3, dados.aposentados().VALOR(), moedaStyle);

        Row row6 = sheet.createRow(6);
        row6.createCell(0).setCellValue("");
        row6.createCell(1).setCellValue("Pensionistas");
        preencherQuantidade(row6, 2, dados.pensionistas().QUANTIDADE());
        preencherValor(row6, 3, dados.pensionistas().VALOR(), moedaStyle);

        Row row7 = sheet.createRow(7);
        row7.createCell(0).setCellValue("");
        row7.createCell(1).setCellValue("Estagiários");
        preencherQuantidade(row7, 2, dados.estagiarios().QUANTIDADE());
        preencherValor(row7, 3, dados.estagiarios().VALOR(), moedaStyle);

        Row row8 = sheet.createRow(8);
        row8.createCell(0).setCellValue("");
        row8.createCell(1).setCellValue("Cedidos/Recebidos");
        preencherQuantidade(row8, 2, dados.cedidosRecebidos().QUANTIDADE());
        preencherValor(row8, 3, dados.cedidosRecebidos().VALOR(), moedaStyle);

        Row row9 = sheet.createRow(9);
        row9.createCell(0).setCellValue("");
        row9.createCell(1).setCellValue("Temporários");
        preencherQuantidade(row9, 2, dados.temporarios().QUANTIDADE());
        preencherValor(row9, 3, dados.temporarios().VALOR(), moedaStyle);

        Row row10 = sheet.createRow(10);
        row10.createCell(0).setCellValue("");
        row10.createCell(1).setCellValue("Agentes Políticos");
        preencherQuantidade(row10, 2, dados.agentePolitico().QUANTIDADE());
        preencherValor(row10, 3, dados.agentePolitico().VALOR(), moedaStyle);

        Row row11 = sheet.createRow(11);
        criarCelulaComCor(row11, 0, "", corFundo);
        criarCelulaComCor(row11, 1, "TOTAL DETALHADO NO PORTAL", corFundo);
        criarCelulaComCor(row11, 2, quantidadeTotal.intValue(), corFundo);
        preencherValor(row11, 3, valorTotal, moedaComCorStyle);

        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(output);
        workbook.close();
        return output.toByteArray();

    }
}
