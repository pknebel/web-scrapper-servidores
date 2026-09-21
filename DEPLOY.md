# Publicando o web scrapper de graça

Objetivo: deixar back (Spring Boot 4 / Java 25) e front (Vite) acessíveis pela
internet por algumas semanas, sem custo, para uma pessoa externa validar os dados.

| Camada | Plataforma | Custo | Observação |
|---|---|---|---|
| Frontend (Vite) | **Vercel** | grátis, permanente | build automático a cada push |
| Backend (Spring Boot) | **Render** (plano free, via Docker) | grátis, 750 h/mês | hiberna após 15 min sem tráfego |

Plano B (mais RAM, se a geração de planilha estourar memória): **Google Cloud Run**.
Plano C (mais rápido de montar, depende da sua máquina ligada): **Cloudflare Tunnel**.

---

## 1. Preparar o backend

Três arquivos foram adicionados na raiz do repositório:

- `Dockerfile` — build multi-stage usando o próprio `mvnw` do projeto (JDK 25 no
  build, JRE 25 no runtime) e flags de JVM dimensionadas para 512 MB.
- `.dockerignore` — mantém `target/`, `.git/` e afins fora da imagem.
- `render.yaml` — blueprint opcional do Render.

E `src/main/resources/application.properties` passou a ler duas coisas do ambiente:

```properties
server.port=${PORT:8080}
app.cors.origem=${APP_CORS_ORIGEM:http://localhost:5173}
```

Ambas mantêm o comportamento atual quando você roda local — sem variável de
ambiente, continua 8080 e `localhost:5173`.

Teste a imagem antes de subir (opcional, mas economiza tempo):

```bash
docker build -t scrapper-api .
docker run --rm -p 8080:8080 -e PORT=8080 scrapper-api
```

Commit e push:

```bash
git add Dockerfile .dockerignore render.yaml src/main/resources/application.properties DEPLOY.md
git commit -m "Adiciona configuracao de deploy (Docker + Render)"
git push
```

## 2. Subir o backend no Render

1. Crie a conta em <https://render.com> (login com GitHub, sem cartão).
2. **New → Web Service** → conecte o repositório do backend.
3. Configure:
   - **Language/Runtime:** `Docker`
   - **Instance Type:** `Free`
   - **Region:** `Oregon` (ou `Frankfurt`)
   - **Branch:** `main`
4. Em **Environment**, adicione:
   - `TZ` = `America/Sao_Paulo`
   - `APP_CORS_ORIGEM` = deixe vazio por enquanto (volta no passo 4)
5. **Create Web Service**. O primeiro build leva de 5 a 10 min (baixa o Maven e
   todas as dependências). Ao final você recebe uma URL do tipo
   `https://web-scrapper-servidores-api.onrender.com`.
6. Valide chamando um endpoint direto no navegador antes de mexer no front.

> Se preferir, use **New → Blueprint** e o Render lê o `render.yaml` sozinho.

## 3. Subir o frontend na Vercel

1. Conta em <https://vercel.com> com o mesmo GitHub.
2. **Add New → Project** → repositório do front. A Vercel detecta Vite sozinha
   (`npm run build`, saída `dist`).
3. Em **Environment Variables**, adicione a URL do backend. O nome depende de como
   o código chama — em projeto Vite costuma ser algo como:
   - `VITE_API_URL` = `https://web-scrapper-servidores-api.onrender.com`

   Confira no código do front qual variável é lida (`import.meta.env.VITE_...`).
   Se a URL da API estiver escrita fixa no código, troque por
   `import.meta.env.VITE_API_URL` antes do deploy.
4. **Deploy**. Sai uma URL tipo `https://web-scrapper-front.vercel.app`.

## 4. Fechar o CORS

Volte no Render → seu serviço → **Environment** → `APP_CORS_ORIGEM` =
`https://web-scrapper-front.vercel.app` (sem barra no final). Salve; o Render
reinicia o serviço sozinho.

Pronto — mande a URL da Vercel para a pessoa que vai validar.

---

## O que esperar (e como contornar)

**Cold start.** No plano free o Render desliga o serviço após 15 minutos sem
requisição. A primeira chamada depois disso demora de 40 a 90 segundos — Spring
Boot subindo do zero. A pessoa vai achar que travou. Duas saídas:

- Avisar antes: "a primeira tela demora ~1 min, depois fica rápido".
- Manter acordado com um ping externo (ex.: <https://uptimerobot.com>, grátis) a
  cada 10 min. Cabe no limite: 750 h/mês cobrem um único serviço 24/7 (~730 h).

**Memória.** 512 MB é apertado para Spring Boot + Apache POI gerando planilha.
As flags do Dockerfile (`MaxRAMPercentage=70`, `SerialGC`) ajudam, mas se aparecer
`OOMKilled` ou erro 502 na hora de baixar a planilha, vá para o Plano B.

**Consulta anual longa.** O `BuscaAnualService` dispara ~120 requisições ao portal.
O Render não corta requisições longas (é um servidor persistente, não serverless),
então isso funciona — mas o cache de 10 min do `application.properties` é o que
evita refazer tudo quando a pessoa consulta e depois baixa a planilha. Mantenha.

**Geobloqueio do portal.** O `transparencia.e-publica.net` será acessado a partir
de um IP dos EUA (ou Alemanha). Se o portal bloquear ou responder diferente para
IP de fora do Brasil, a consulta falha em produção e funciona na sua máquina.
**Teste isso primeiro** — é o único risco que invalida o plano inteiro. Se
acontecer, use o Plano C.

---

## Plano B — Google Cloud Run (se faltar memória)

Escala a zero como o Render, mas deixa você pedir 1 GiB de RAM. A cota gratuita
mensal (180.000 vCPU-s, 360.000 GiB-s e 2 milhões de requisições) é folgada para
esse uso. Exige conta de faturamento com cartão — dentro da cota não cobra, mas o
cartão é obrigatório.

```bash
gcloud run deploy web-scrapper-api \
  --source . \
  --region southamerica-east1 \
  --memory 1Gi \
  --timeout 900 \
  --allow-unauthenticated \
  --set-env-vars APP_CORS_ORIGEM=https://web-scrapper-front.vercel.app,TZ=America/Sao_Paulo
```

`southamerica-east1` (São Paulo) resolve de quebra o risco de geobloqueio, já que
o IP de saída fica no Brasil. O `--timeout 900` dá 15 min de folga para a consulta
anual. O mesmo `Dockerfile` é usado.

## Plano C — Cloudflare Tunnel (o mais rápido, sem nuvem)

Se o portal bloquear IP estrangeiro, ou se você quiser um link público hoje sem
configurar nada: rode back e front na sua máquina e exponha por túnel.

```bash
# Windows: winget install --id Cloudflare.cloudflared
cloudflared tunnel --url http://localhost:8080
```

Devolve uma URL `https://algo-aleatorio.trycloudflare.com` na hora, sem conta e
sem custo. Duas ressalvas: só funciona com sua máquina ligada e com a aplicação
rodando, e a URL muda a cada reinício (para uma URL fixa é preciso conta
Cloudflare gratuita + domínio próprio).

Como o front também precisa de túnel, o caminho mais simples é rodar o Vite em
modo build servido pelo próprio Spring Boot (colocando o `dist` em
`src/main/resources/static`), assim um único túnel resolve os dois.

---

## Alternativas descartadas e por quê

- **Fly.io** — não oferece mais tier gratuito para contas novas desde 2025.
- **Railway** — só US$ 5 de crédito inicial; depois é pago.
- **Koyeb** — tem free (512 MB, 1 serviço), mas só nas regiões Frankfurt e
  Washington e com scale-to-zero obrigatório após 1 h. É um substituto válido do
  Render se você já tiver conta lá.
- **Vercel/Netlify para o backend** — funções serverless com timeout de 60 s a
  5 min e sem processo persistente; não combinam com a consulta anual longa nem
  com a geração de planilha em memória.
- **Oracle Cloud Always Free** — é a única opção realmente permanente e com RAM
  de sobra (ARM, 24 GB), mas exige aprovação de conta, cartão e administração de
  VM. Desproporcional para algumas semanas de validação.

---

## Fontes

- [Render — plataformas com free tier real em 2026](https://render.com/articles/platforms-with-a-real-free-tier-for-developers-in-2026)
- [Cloud Run — pricing e cota gratuita](https://cloud.google.com/run/pricing)
- [Fly.io — planos descontinuados](https://fly.io/docs/about/discontinued-plans/)
- [Koyeb — free tier 2026](https://www.srvrlss.io/provider/koyeb/)
