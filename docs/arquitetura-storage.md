# Arquitetura de Storage — Integration Engine

> **Status:** Aguardando homologação em HML antes de prosseguir.
> Última atualização: Sprint 5 concluída.

---

## 1. Contexto

A aplicação gera dois tipos de relatório por importação executada:

| Tipo | Formato | Uso |
|------|---------|-----|
| Relatório HTML | `.html` | Visualização no navegador (inline) |
| Relatório Excel | `.xlsx` | Download para análise (attachment) |

Após a geração, os arquivos são salvos em disco e disponibilizados via endpoint `GET /download/{nomeArquivo}`.

---

## 2. Estado atual (pós Sprint 5)

### 2.1 Fluxo de dados

```
POST /importar/clientes  ou  POST /importar/funcionarios
  └─► ImportacaoClienteController
       └─► ClienteImportService.processar()   (ou FuncionarioImportService)
            ├─► ImportacaoReportGenerator.gerarRelatorio()
            │    ├─► Sanitiza nome via StringUtils.sanitizarNomeArquivo()
            │    ├─► Cria diretório via Files.createDirectories(Path.of(outputDir))
            │    └─► Grava HTML via BufferedWriter/FileWriter → disco local
            │
            ├─► ExcelReportGenerator.gerarExcel()
            │    ├─► Sanitiza nome via StringUtils.sanitizarNomeArquivo()
            │    ├─► Cria diretório via Files.createDirectories(Path.of(outputDir))
            │    └─► Grava XLSX via FileOutputStream → disco local
            │
            └─► ProcessamentoResult
                 ├─► caminhoRelatorio : Path  (caminho absoluto do HTML no disco)
                 └─► caminhoExcel     : Path  (caminho absoluto do XLSX no disco)

ImportacaoClienteController.toResponse()
  ├─► downloadUrl = baseUrl + "/download/" + caminhoRelatorio.getFileName()
  └─► excelUrl    = baseUrl + "/download/" + caminhoExcel.getFileName()

GET /download/{nomeArquivo}
  └─► DownloadController.download()
       ├─► resolve: Path.of(outputDir).resolve(nomeArquivo)
       ├─► FileSystemResource(caminho)
       └─► ResponseEntity com Content-Disposition e MediaType corretos
```

### 2.2 Propriedades configuráveis

| Propriedade | Padrão | Propósito |
|-------------|--------|-----------|
| `app.base-url` | `http://localhost:8080` | Base das URLs absolutas retornadas ao frontend |
| `app.report.output-dir` | `output` | Diretório de saída dos relatórios |
| `app.historico.arquivo` | `data/historico.json` | Arquivo JSON de histórico de importações |

Em HML e prod, esses valores devem ser configurados via variáveis de ambiente:

| Variável | Mapeamento |
|----------|-----------|
| `APP_BASE_URL` | `app.base-url` |
| `REPORT_OUTPUT_DIR` | `app.report.output-dir` |
| `HISTORICO_ARQUIVO` | `app.historico.arquivo` |

### 2.3 Pacotes criados (Sprint 4 e 5 — inativos no fluxo atual)

```
core/
  domain/importacao/
    TipoImportacao.java          ← enum CLIENTES | FUNCIONARIOS
    RegistroImportacao.java      ← entidade de domínio de uma importação
  repository/
    ImportacaoRepository.java    ← interface de persistência do histórico
    impl/
      InMemoryImportacaoRepository.java  ← implementação em memória + JSON
  storage/
    RelatorioArmazenado.java     ← value object: resultado de um armazenamento
    RelatorioStorage.java        ← interface de armazenamento de relatórios
    local/
      LocalRelatorioStorage.java ← adaptador local (definido, não conectado)
```

`LocalRelatorioStorage` é um `@Component` registrado no contexto Spring mas
não injetado em nenhum gerador. Os geradores continuam usando `FileOutputStream`
e `BufferedWriter` diretamente até a Sprint 6 ser aprovada.

---

## 3. Arquitetura proposta (Sprint 6 em diante)

### 3.1 Objetivo

Desacoplar a **geração** do relatório (construção do conteúdo) do **armazenamento**
(onde o conteúdo é gravado), permitindo trocar o backend de storage sem alterar
nenhuma regra de negócio.

### 3.2 Interface central

```java
// br.com.buni.integration.core.storage.RelatorioStorage
public interface RelatorioStorage {
    RelatorioArmazenado salvar(String nomeArquivo, byte[] conteudo, String contentType);
    Resource recuperar(String referencia);
    boolean existe(String referencia);
}
```

### 3.3 Diagrama de implementações

```
                ┌──────────────────────────────────────────────┐
                │         RelatorioStorage  (interface)         │
                │  salvar(nome, bytes, contentType)             │
                │  recuperar(referencia): Resource              │
                │  existe(referencia): boolean                  │
                └───────────────────┬──────────────────────────┘
                                    │ implements
          ┌─────────────────────────┼──────────────────┐
          ▼                         ▼                   ▼
LocalRelatorioStorage    AzureBlobRelatorioStorage   S3RelatorioStorage
(Sprint 6)               (Sprint futura)             (Sprint futura)
app.report.output-dir    AZURE_STORAGE_*             AWS_S3_*
```

### 3.4 Fluxo após Sprint 6

```
ExcelReportGenerator
  ├─► build XSSFWorkbook
  ├─► ByteArrayOutputStream → byte[]
  └─► RelatorioStorage.salvar(nome, bytes, contentType)
       └─► RelatorioArmazenado { referencia, tamanhoBytes }

ImportacaoReportGenerator
  ├─► build HTML → String → byte[]
  └─► RelatorioStorage.salvar(nome, bytes, contentType)
       └─► RelatorioArmazenado { referencia, tamanhoBytes }

ProcessamentoResult
  ├─► relatorioHtml  : RelatorioArmazenado   (era: Path caminhoRelatorio)
  └─► relatorioExcel : RelatorioArmazenado   (era: Path caminhoExcel)

GET /download/{referencia}
  └─► DownloadController
       ├─► RelatorioStorage.existe(referencia)
       └─► RelatorioStorage.recuperar(referencia) → Resource
```

### 3.5 Classes que mudam na Sprint 6

| Classe | Mudança | Impacto |
|--------|---------|---------|
| `ExcelReportGenerator` | `FileOutputStream` → `RelatorioStorage.salvar()` | Médio |
| `ImportacaoReportGenerator` | `BufferedWriter` → `RelatorioStorage.salvar()` | Médio |
| `ProcessamentoResult` | `Path` → `RelatorioArmazenado` nos dois campos | Médio |
| `DownloadController` | `FileSystemResource` → `RelatorioStorage.recuperar/existe` | Pequeno |
| `HistoricoService` | lê `referencia` em vez de `Path.getFileName()` | Pequeno |
| `ImportacaoClienteController` | lê `referencia` para construir `downloadUrl` | Pequeno |

**Não mudam:** toda lógica de negócio dos services, connectors, readers de CSV/Excel,
validators, mappers, `HistoricoImportacao`, `RegistroImportacao`, `ImportacaoRepository`.

---

## 4. Checklist de homologação em HML

Use esta lista para validar a aplicação antes de retomar a Sprint 6.

### 4.1 Configuração do ambiente

- [ ] Variável `APP_BASE_URL` configurada com a URL pública da API em HML
- [ ] Variável `REPORT_OUTPUT_DIR` configurada (ou validado que `output` é acessível)
- [ ] Variável `HISTORICO_ARQUIVO` configurada (ou validado que `data/historico.json` é acessível)
- [ ] Deploy realizado com perfil `hml` (`SPRING_PROFILES_ACTIVE=hml`)

### 4.2 Geração e download de relatórios

- [ ] Upload de arquivo CSV de clientes retorna HTTP 200
- [ ] Resposta JSON contém `downloadUrl` com URL absoluta (ex: `https://api.hml.com/download/...`)
- [ ] Resposta JSON contém `excelUrl` com URL absoluta
- [ ] `GET {downloadUrl}` retorna HTTP 200 com `Content-Type: text/html`
- [ ] `GET {excelUrl}` retorna HTTP 200 com `Content-Type: application/vnd.openxmlformats-...`
- [ ] Download do `.xlsx` abre corretamente no Excel / LibreOffice
- [ ] Relatório HTML exibe dados corretamente no navegador
- [ ] Upload de arquivo CSV de funcionários retorna HTTP 200 e gera relatórios
- [ ] Upload de arquivo XLSX retorna HTTP 200 e gera relatórios

### 4.3 Nomes dos arquivos (Sprint 3 — sanitização)

- [ ] Arquivo com acentos no nome (`funcionários.csv`) gera relatório com nome limpo
- [ ] Arquivo com espaços no nome (`base clientes 2026.csv`) gera nome sem espaços
- [ ] Arquivo com parênteses (`clientes (junho).csv`) gera nome sem parênteses
- [ ] `downloadUrl` e `excelUrl` contêm apenas caracteres `[a-zA-Z0-9_\-.]`
- [ ] Dupla extensão (`arquivo.csv.csv`) é tratada corretamente

### 4.4 Histórico de importações

- [ ] `GET /importacoes` retorna a lista com os registros das importações realizadas
- [ ] `GET /importacoes/{id}` retorna o registro correto
- [ ] Histórico persiste após reinício da aplicação (se houver volume configurado)

### 4.5 Logs de diagnóstico (Sprint 2)

Nos logs do servidor, após uma importação, devem aparecer:

```
[EXCEL] Relatório gerado — path=<caminho-absoluto> | tamanho=<N> bytes
[HTML]  Relatório gerado — path=<caminho-absoluto> | tamanho=<N> bytes
[DOWNLOAD] Solicitado — arquivo=<nome> | diretório=<dir> | caminho-resolvido=<path>
[DOWNLOAD] Arquivo localizado — caminho=<path> | tamanho=<N> bytes
[HISTORICO] Registro salvo — id=<importId> | tipo=<tipo> | arquivo=<nome> | status=<status>
```

Se aparecer `[DOWNLOAD] Arquivo não encontrado`, verificar:
- o diretório listado no log é o mesmo onde os relatórios foram salvos
- o `REPORT_OUTPUT_DIR` está consistente entre os logs do gerador e do download

### 4.6 Segurança

- [ ] `GET /download/../etc/passwd` retorna HTTP 400 (path traversal bloqueado)
- [ ] `GET /download/arquivo-inexistente.xlsx` retorna HTTP 404

---

## 5. Roadmap de Sprints

| Sprint | Objetivo | Status |
|--------|----------|--------|
| Sprint 1 | URLs absolutas com `app.base-url` | ✅ Concluída |
| Sprint 2 | Diretório configurável + logs de diagnóstico | ✅ Concluída |
| Sprint 3 | Sanitização centralizada de nomes em `StringUtils` | ✅ Concluída |
| Sprint 4 | Domínio + repositório de histórico (`ImportacaoRepository`) | ✅ Concluída |
| Sprint 5 | Interface `RelatorioStorage` + adaptador local (desconectado) | ✅ Concluída |
| — | **Homologação em HML** | ⏳ Em andamento |
| Sprint 6 | Conectar `LocalRelatorioStorage` nos geradores | 🔒 Aguardando HML |
| Sprint 7 | Azure Blob Storage | 🔒 Aguardando Sprint 6 |
| Sprint 8 | AWS S3 | 🔒 Aguardando decisão |
