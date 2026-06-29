# Portal de Importação B.uni

Sistema de importação em lote de clientes via arquivo CSV, com validação de dados, consulta de CPF na API B.uni, controle de duplicidade, inclusão automatizada e geração de relatório detalhado de processamento.

---

## Sumário

- [Objetivo](#objetivo)
- [Arquitetura](#arquitetura)
- [Fluxo Funcional](#fluxo-funcional)
- [Formato do CSV](#formato-do-csv)
- [Regras de Negócio](#regras-de-negócio)
- [Relatório de Processamento](#relatório-de-processamento)
- [Status Geral do Processamento](#status-geral-do-processamento)
- [Como Executar Localmente](#como-executar-localmente)
- [Configurações](#configurações)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Troubleshooting](#troubleshooting)
- [Próximas Melhorias](#próximas-melhorias)

---

## Objetivo

O Portal de Importação B.uni automatiza o processo de cadastro de clientes (pessoa física) na plataforma B.uni a partir de uma base de dados exportada em CSV pelo sistema de RH.

A cada execução, o sistema:

1. Lê e valida o arquivo CSV recebido
2. Detecta e descarta CPFs duplicados no próprio arquivo
3. Consulta a API B.uni para verificar se o cliente já existe na base
4. Inclui clientes novos via API
5. Registra o resultado de cada linha em um relatório CSV detalhado
6. Exibe o resumo do processamento no frontend com opção de download do relatório

---

## Arquitetura

O projeto segue uma arquitetura em camadas com separação clara de responsabilidades:

```
Frontend (HTML/CSS/JS)
        │
        ▼
   Controller          ← Recebe o upload, orquestra a resposta HTTP
        │
        ▼
    Service            ← Coordena o fluxo de processamento linha a linha
   ┌────┴────┐
   │         │
Validator  Mapper      ← Valida dados do CSV / Converte CSV para Request da API
   │         │
   └────┬────┘
        │
    Connector          ← Integração com APIs externas (Auth, Cliente, ViaCEP)
        │
  ReportGenerator      ← Gera o arquivo CSV de relatório de execução
```

### Camadas

| Camada | Classe | Responsabilidade |
|--------|--------|-----------------|
| Controller | `ImportacaoClienteController` | Recebe o arquivo CSV via `multipart/form-data`, retorna resumo JSON e serve download do relatório |
| Controller | `HealthController` | Endpoint `/health` para verificação de saúde da aplicação |
| Service | `ClienteImportService` | Orquestra todo o processamento: deduplicação, validação, consulta, inclusão e montagem do relatório |
| Service | `CsvService` | Lê e faz o parsing do arquivo CSV usando OpenCSV |
| Connector | `BuniAuthConnector` | Gera e cacheia o token OAuth para autenticação na API B.uni |
| Connector | `BuniClienteConnector` | Consulta CPF e inclui cliente via API B.uni |
| Connector | `CepConnector` | Enriquece dados de endereço via API ViaCEP |
| Mapper | `ClienteMapper` | Converte `ClienteCsv` para `ClienteRequest`, aplicando transformações e fallbacks |
| Validator | `ClienteCsvValidator` | Valida campos obrigatórios, formato de CPF (incluindo dígitos verificadores) e e-mail |
| Report | `ImportacaoReportGenerator` | Gera o arquivo CSV de relatório com o resultado linha a linha |
| Config | `RestTemplateConfig` | Configura o `RestTemplate` com timeouts de conexão e leitura |
| Logging | `LogService` | Centraliza os logs estruturados de início, sucesso, erro e fim de processamento |

---

## Fluxo Funcional

```
[Usuário]
    │
    │  1. Seleciona arquivo CSV no frontend e clica em "Processar Base"
    ▼
[POST /importar/clientes]
    │
    │  2. CsvService lê e parseia o arquivo (separador ";", encoding UTF-8)
    ▼
[Para cada linha do CSV]
    │
    ├── 3. Normaliza e valida o CPF
    │       └── CPF vazio ou inválido → ERRO, próxima linha
    │
    ├── 4. Verifica duplicidade no próprio arquivo
    │       └── CPF já processado nesta execução → DUPLICADO, próxima linha
    │
    ├── 5. Valida os campos obrigatórios (ClienteCsvValidator)
    │       └── Campos inválidos → ERRO, próxima linha
    │
    ├── 6. Consulta CPF na API B.uni (BuniClienteConnector)
    │       ├── HTTP 404 ou código 100000026 → cliente não existe, segue para inclusão
    │       └── Cliente encontrado → DUPLICADO, próxima linha
    │
    ├── 7. Mapeia CSV para Request da API (ClienteMapper)
    │       └── Enriquece endereço via ViaCEP (com fallback se CEP não encontrado)
    │
    └── 8. Inclui cliente na API B.uni
            ├── Sucesso → SUCESSO
            └── Erro de API → ERRO com código HTTP real registrado

    │
    ▼
[Após processar todas as linhas]
    │
    ├── 9.  Calcula status geral (SUCESSO, FALHA, PROCESSADO_COM_DUPLICIDADES, etc.)
    ├── 10. Gera relatório CSV em /output/
    └── 11. Retorna JSON com resumo + URL de download para o frontend
```

---

## Formato do CSV

| Item | Valor |
|------|-------|
| Formato aceito | `.csv` |
| Separador | `;` (ponto e vírgula) |
| Encoding recomendado | `UTF-8` |
| Cabeçalho | Obrigatório na primeira linha |

### Colunas esperadas

| Coluna no CSV | Campo | Obrigatório |
|---------------|-------|-------------|
| `CPF` | CPF do cliente | ✅ |
| `NOME` | Nome completo | ✅ |
| `DTNASCIMENTO` | Data de nascimento (`dd/MM/yyyy`) | ✅ |
| `CEP` | CEP do endereço | ✅ |
| `SALARIO` | Salário (formato BR: `1.234,56`) | ✅ |
| `CODAGENCIAPAGTO` | Código da agência bancária | ✅ |
| `CONTAPAGAMENTO` | Número da conta (com ou sem dígito após `-`) | ✅ |
| `EMAILPESSOAL` | E-mail pessoal | ✅ (ou profissional) |
| `EMAILPROFISSIONAL` | E-mail profissional | ✅ (ou pessoal) |
| `CNPJ` | CNPJ da empresa | ❌ |
| `NOMEFANTASIA` | Nome da empresa | ❌ |
| `CHAPA` | Matrícula do funcionário | ❌ |
| `DATAADMISSAO` | Data de admissão (`dd/MM/yyyy`) | ❌ |
| `FUNCAO` | Cargo / função | ❌ |
| `GRAUINSTRUCAO` | Escolaridade | ❌ |
| `SITUACAO` | Situação do funcionário | ❌ |
| `RUA` | Logradouro | ❌ |
| `NUMERO` | Número do endereço | ❌ |
| `BANCO` | Código do banco | ❌ |
| `CODBANCOPAGTO` | Código de compensação bancária | ❌ |
| `TELEFONE1` | Telefone principal (com DDD) | ❌ |
| `TELEFONE2` | Telefone secundário | ❌ |
| `SEXO` | Sexo (`M` ou `F`) | ❌ |
| `CODSECAO` | Código da seção/departamento | ❌ |
| `HORARIO` | Horário de trabalho | ❌ |

> **Nota:** Se o arquivo CSV do sistema de RH utilizar encoding `ISO-8859-1` ou `Windows-1252`, altere a propriedade `buni.csv.charset` no `application.properties`.

---

## Regras de Negócio

### Validação de CPF
- CPF vazio → linha rejeitada com status `ERRO`
- CPF com número de dígitos diferente de 11 → `ERRO`
- CPF com dígitos verificadores inválidos (algoritmo padrão brasileiro) → `ERRO`
- CPFs com todos os dígitos iguais (ex: `00000000000`) → `ERRO`

### Deduplicação
- CPF que aparece mais de uma vez **no mesmo arquivo** → segunda ocorrência marcada como `DUPLICADO` e ignorada
- CPF que **já existe na base B.uni** → marcado como `DUPLICADO` e não reenviado

### Consulta de CPF na API
- Resposta HTTP `404` → cliente não encontrado, segue para inclusão
- Código `100000026` no body da resposta → cliente não encontrado, segue para inclusão
- Qualquer outro erro HTTP → registrado como `ERRO` no relatório

### Enriquecimento de Endereço
- O sistema consulta o ViaCEP com o CEP informado no CSV para obter bairro, cidade e UF
- Se o ViaCEP não retornar dados, aplica fallback: bairro e cidade como `NAO INFORMADO`, UF como `SP`
- O campo `FallbackAplicado` no relatório indica se o fallback foi utilizado

### E-mail
- O sistema prioriza `EMAILPESSOAL`; utiliza `EMAILPROFISSIONAL` apenas se o pessoal estiver ausente
- Ao menos um dos dois é obrigatório

### Dados Bancários
- O dígito da agência não é fornecido pelo CSV; valor fixo `0` aplicado por padrão (comportamento de homologação)
- O dígito da conta é extraído automaticamente após o caractere `-` no campo `CONTAPAGAMENTO`

---

## Relatório de Processamento

Gerado automaticamente em `output/` após cada processamento.  
Nome do arquivo: `relatorio_[timestamp]_[nome-do-csv].csv`

### Colunas do relatório

| Coluna | Descrição |
|--------|-----------|
| `DataHora` | Data e hora exata do processamento da linha |
| `Arquivo` | Nome do arquivo CSV enviado |
| `LinhaCSV` | Número da linha no arquivo original |
| `CPF` | CPF normalizado (11 dígitos) |
| `Nome` | Nome do cliente |
| `Status` | `SUCESSO`, `ERRO` ou `DUPLICADO` |
| `CampoErro` | Campo que causou o erro de validação |
| `ValorRecebido` | Valor problemático recebido no CSV |
| `MotivoErro` | Descrição do motivo do erro |
| `AcaoSugerida` | Orientação para correção |
| `CodigoCliente` | Código retornado pela API após inclusão bem-sucedida |
| `MensagemApi` | Mensagem retornada pela API B.uni |
| `FallbackAplicado` | `true` se o endereço foi preenchido com dados de fallback |
| `Observacao` | Informações adicionais sobre o processamento da linha |

### Status por linha

| Status | Significado |
|--------|-------------|
| `SUCESSO` | Cliente incluído com sucesso na API |
| `ERRO` | Falha na validação dos dados ou erro retornado pela API |
| `DUPLICADO` | CPF duplicado no arquivo ou já existente na base |

---

## Status Geral do Processamento

Retornado no campo `status` da resposta JSON após o processamento.

| Status | Condição |
|--------|----------|
| `SUCESSO` | Todos os registros incluídos com sucesso (sem erros e sem duplicados) |
| `PROCESSADO_COM_ALERTAS` | Ao menos um sucesso, mas com erros ou duplicados |
| `PROCESSADO_COM_DUPLICIDADES` | Nenhum sucesso, apenas duplicados (sem erros) |
| `FALHA` | Nenhum sucesso, apenas erros (sem duplicados) |
| `FALHA_COM_DUPLICIDADES` | Nenhum sucesso, com erros e duplicados |
| `SEM_REGISTROS_PROCESSADOS` | Arquivo vazio ou sem linhas válidas |

---

## Como Executar Localmente

### Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java | 17 |
| Maven | 3.8+ |
| Acesso à API B.uni | Ambiente de homologação |
| Acesso à internet | Para consultas ao ViaCEP |

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd integration-engine
```

### 2. Configurar credenciais

Defina as variáveis de ambiente antes de subir a aplicação:

```bash
# Linux / macOS
export BUNI_CLIENT_ID=955000
export BUNI_CLIENT_SECRET=<client-secret>
export BUNI_USERNAME=USR_API
export BUNI_PASSWORD=<senha>
```

```powershell
# Windows (PowerShell)
$env:BUNI_CLIENT_ID = "955000"
$env:BUNI_CLIENT_SECRET = "<client-secret>"
$env:BUNI_USERNAME = "USR_API"
$env:BUNI_PASSWORD = "<senha>"
```

> Se não definir as variáveis, os valores padrão do `application.properties` serão usados (apenas para homologação local).

### 3. Executar a aplicação

```bash
mvn spring-boot:run
```

### 4. Acessar o frontend

```
http://localhost:8080
```

### 5. Testar via Postman

```
POST http://localhost:8080/importar/clientes
Content-Type: multipart/form-data
Body → form-data → Key: file | Type: File | Value: <arquivo.csv>
```

### 6. Verificar saúde da aplicação

```
GET http://localhost:8080/health
```

### 7. Baixar relatório gerado

```
GET http://localhost:8080/importar/relatorios/{nome-do-relatorio.csv}
```

---

## Configurações

Arquivo: `src/main/resources/application.properties`

| Propriedade | Descrição | Padrão |
|-------------|-----------|--------|
| `server.port` | Porta da aplicação | `8080` |
| `buni.ambiente` | Identificador do ambiente | `HML` |
| `buni.csv.charset` | Encoding do arquivo CSV | `UTF-8` |
| `buni.auth.url` | URL do endpoint de autenticação OAuth | _(URL de homologação)_ |
| `buni.auth.client-id` | Client ID OAuth | `${BUNI_CLIENT_ID}` |
| `buni.auth.client-secret` | Client Secret OAuth | `${BUNI_CLIENT_SECRET}` |
| `buni.auth.grant-type` | Grant type OAuth | `filicenca` |
| `buni.auth.username` | Usuário da API | `${BUNI_USERNAME}` |
| `buni.auth.password` | Senha da API | `${BUNI_PASSWORD}` |
| `buni.cliente.inclusao.url` | URL de inclusão de cliente | _(URL de homologação)_ |
| `buni.cliente.consulta-cpf.url` | URL de consulta por CPF | _(URL de homologação)_ |
| `spring.servlet.multipart.max-file-size` | Tamanho máximo do arquivo | `50MB` |

### Diretórios

| Diretório | Finalidade |
|-----------|-----------|
| `input/` | Diretório reservado para arquivos CSV de entrada (uso local/referência) |
| `output/` | Relatórios CSV gerados após cada processamento (criado automaticamente) |
| `logs/` | Arquivos de log rotativos da aplicação |

---

## Estrutura de Pastas

```
integration-engine/
├── src/
│   ├── main/
│   │   ├── java/br/com/buni/integration/core/
│   │   │   ├── IntegrationEngineApplication.java
│   │   │   ├── config/
│   │   │   │   └── RestTemplateConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ImportacaoClienteController.java
│   │   │   │   └── HealthController.java
│   │   │   ├── service/
│   │   │   │   ├── ClienteImportService.java
│   │   │   │   └── CsvService.java
│   │   │   ├── connector/
│   │   │   │   ├── BuniAuthConnector.java
│   │   │   │   ├── BuniClienteConnector.java
│   │   │   │   └── CepConnector.java
│   │   │   ├── mapper/
│   │   │   │   └── ClienteMapper.java
│   │   │   ├── validator/
│   │   │   │   └── ClienteCsvValidator.java
│   │   │   ├── report/
│   │   │   │   └── ImportacaoReportGenerator.java
│   │   │   ├── logging/
│   │   │   │   └── LogService.java
│   │   │   ├── model/
│   │   │   │   ├── csv/
│   │   │   │   │   └── ClienteCsv.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ExecutionReportLine.java
│   │   │   │   │   ├── ExecutionReportSummary.java
│   │   │   │   │   └── ProcessamentoResult.java
│   │   │   │   ├── request/
│   │   │   │   │   └── ClienteRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── TokenResponse.java
│   │   │   │       ├── CepResponse.java
│   │   │   │       ├── InclusaoClienteResponse.java
│   │   │   │       └── ConsultaClienteCpfResponse.java
│   │   │   ├── enums/
│   │   │   │   ├── SexoEnum.java
│   │   │   │   ├── EstadoCivilEnum.java
│   │   │   │   ├── EscolaridadeEnum.java
│   │   │   │   ├── NacionalidadeEnum.java
│   │   │   │   ├── TipoDocumentoEnum.java
│   │   │   │   ├── TipoResidenciaEnum.java
│   │   │   │   ├── TipoTelefoneEnum.java
│   │   │   │   └── TipoConsultaClienteEnum.java
│   │   │   ├── exception/
│   │   │   │   └── IntegrationException.java
│   │   │   └── util/
│   │   │       ├── StringUtils.java
│   │   │       ├── DateUtils.java
│   │   │       └── FileUtils.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       └── static/
│   │           ├── index.html
│   │           ├── style.css
│   │           └── app.js
│   └── test/
│       └── java/br/com/buni/integration/core/
│           └── IntegrationEngineApplicationTests.java
├── input/                          ← CSVs de entrada (uso local)
├── output/                         ← Relatórios gerados (criado automaticamente)
├── logs/                           ← Logs rotativos
├── pom.xml
└── README.md
```

---

## Troubleshooting

### Token inválido ou expirado
**Sintoma:** Erro `401 Unauthorized` nos logs ao chamar a API B.uni.  
**Causa:** Credenciais incorretas ou token expirado fora do ciclo de cache.  
**Solução:** Verifique as variáveis de ambiente `BUNI_CLIENT_ID`, `BUNI_CLIENT_SECRET`, `BUNI_USERNAME` e `BUNI_PASSWORD`. O sistema já cacheia e renova o token automaticamente 60 segundos antes do vencimento.

---

### API B.uni indisponível
**Sintoma:** Todos os registros retornam `ERRO` com mensagem de timeout ou conexão recusada.  
**Causa:** API de homologação fora do ar ou instável.  
**Solução:** Verifique o endpoint no `application.properties`. O timeout configurado é de 10s para conexão e 30s para leitura. Aguarde a disponibilidade do ambiente.

---

### CPF não encontrado retornando ERRO em vez de seguir para inclusão
**Sintoma:** Registros que deveriam ser incluídos aparecem como `ERRO`.  
**Causa:** A API retornou um erro diferente de `404` ou código `100000026`.  
**Solução:** Verifique o campo `MotivoErro` no relatório CSV para identificar o código HTTP e a mensagem exata retornada pela API.

---

### CSV com separador incorreto
**Sintoma:** Apenas uma coluna é lida, todos os campos ficam em branco ou a leitura falha.  
**Causa:** O arquivo usa vírgula (`,`) em vez de ponto e vírgula (`;`) como separador.  
**Solução:** Reexporte o CSV com separador `;` no sistema de RH.

---

### CSV com encoding incorreto (acentos corrompidos)
**Sintoma:** Nomes com `ã`, `ç`, `é` aparecem corrompidos na API ou no relatório.  
**Causa:** O arquivo foi exportado com encoding `ISO-8859-1` ou `Windows-1252`, mas a aplicação está lendo como `UTF-8`.  
**Solução:** Altere no `application.properties`:
```properties
buni.csv.charset=ISO-8859-1
```

---

### Relatório não sendo baixado
**Sintoma:** Clique em "Baixar Relatório" não inicia o download ou retorna 404.  
**Causa:** O diretório `output/` não foi criado ou o arquivo foi removido manualmente.  
**Solução:** O diretório é criado automaticamente a cada processamento. Verifique se há permissão de escrita no diretório raiz da aplicação. Reprocesse o arquivo para gerar um novo relatório.

---

### Duplicados não aparecendo corretamente no dashboard
**Sintoma:** Dashboard mostra `totalDuplicado: 0` mesmo com CPFs repetidos.  
**Causa:** O CPF no CSV pode estar com formatação diferente entre as linhas (ex: `123.456.789-09` vs `12345678909`).  
**Solução:** O sistema normaliza o CPF removendo caracteres não numéricos antes de comparar. Se ainda assim não detectar, verifique se o CPF realmente existe na base B.uni ou se está duplicado no arquivo com alguma diferença de espaço ou caractere invisible.

---

## Próximas Melhorias

| Melhoria | Descrição | Prioridade |
|----------|-----------|-----------|
| Testes unitários | Cobertura de `ClienteCsvValidator`, `ClienteMapper` e `ClienteImportService` com JUnit 5 e Mockito | Alta |
| Testes de integração | Testes end-to-end com mock da API B.uni usando WireMock | Alta |
| Observabilidade | Integração com Actuator + Micrometer + exportação de métricas (Prometheus/Grafana) | Média |
| Parametrização por ambiente | Separar `application-hml.properties` e `application-prd.properties` com Spring Profiles | Média |
| Deploy em homologação | Containerização com Docker e configuração de variáveis via `.env` ou secrets | Média |
| Autenticação no frontend | Proteger o portal com login antes de permitir o upload | Média |
| Processamento assíncrono | Para arquivos grandes, mover o processamento para background e notificar por polling ou WebSocket | Baixa |
| Validação de CNPJ | Adicionar validação de dígitos verificadores para o CNPJ da empresa | Baixa |
| Paginação no relatório | Para lotes muito grandes, paginar o relatório ou oferecer visualização online | Baixa |
| Retry automático | Retentar automaticamente inclusões que falharam por instabilidade da API | Baixa |

---

## Tecnologias Utilizadas

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.5.15 | Framework base |
| OpenCSV | 5.9 | Leitura e parsing do CSV |
| Jackson | _(gerenciado pelo Spring)_ | Serialização/deserialização JSON |
| Lombok | 1.18.38 | Redução de boilerplate |
| ViaCEP | API pública | Enriquecimento de endereço por CEP |
| Logback | _(gerenciado pelo Spring)_ | Logging estruturado com rotação de arquivos |

---

> Projeto desenvolvido para o ambiente de homologação B.uni.  
> Em caso de dúvidas sobre os endpoints ou credenciais, consulte a equipe responsável pela API B.uni.
