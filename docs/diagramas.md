# Diagramas de Arquitetura — Portal de Importação B.uni

> Todos os diagramas são gerados com [Mermaid](https://mermaid.js.org/) e renderizam diretamente no GitHub.  
> Para edição visual, utilize o arquivo `arquitetura.drawio` na mesma pasta.

---

## 1. Arquitetura Geral

Visão completa de todos os componentes do sistema e suas integrações.

```mermaid
graph TB
    subgraph USUARIO["👤 Usuário"]
        U([Usuário])
        FE["🌐 Frontend\nHTML / CSS / JS"]
    end

    subgraph BACKEND["⚙️ Backend — Integration Engine (Spring Boot)"]
        subgraph CTRL["Camada Controller"]
            IC["ImportacaoClienteController"]
            HC["HealthController"]
        end
        subgraph SVC["Camada Service"]
            CSV["CsvService"]
            CIS["ClienteImportService"]
            LOG["LogService"]
        end
        subgraph PROC["Validação e Mapeamento"]
            VAL["ClienteCsvValidator"]
            MAP["ClienteMapper"]
        end
        subgraph CONN["Camada Connector"]
            AUTH["BuniAuthConnector"]
            BUNI["BuniClienteConnector"]
            CEP["CepConnector"]
        end
        subgraph RPT["Relatório"]
            RG["ImportacaoReportGenerator"]
        end
    end

    subgraph EXT["🌍 APIs Externas"]
        AUTHAPI["API B.uni\nOAuth Token"]
        CLIAPI["API B.uni\nCliente PF"]
        VCEP["API ViaCEP"]
    end

    subgraph STORAGE["💾 Armazenamento"]
        CSVIN[("CSV Entrada\ninput/")]
        CSVOUT[("Relatório CSV\noutput/")]
        LOGS[("Logs\nlogs/")]
    end

    U --> FE
    FE -->|"POST /importar/clientes"| IC
    FE -->|"GET /importar/relatorios/"| IC
    FE -->|"GET /health"| HC

    IC --> CSV
    IC --> CIS
    CSV --> CSVIN

    CIS --> VAL
    CIS --> MAP
    CIS --> BUNI
    CIS --> RG
    CIS --> LOG

    MAP --> CEP
    BUNI --> AUTH

    AUTH -->|"POST oauth/token"| AUTHAPI
    BUNI -->|"GET ClientePorCPF"| CLIAPI
    BUNI -->|"POST IncluirCliente"| CLIAPI
    CEP -->|"GET ws/CEP/json"| VCEP

    RG --> CSVOUT
    LOG --> LOGS

    style USUARIO fill:#dae8fc,stroke:#6c8ebf
    style BACKEND fill:#f9f9f9,stroke:#999
    style EXT fill:#e1d5e7,stroke:#9673a6
    style STORAGE fill:#f5f5f5,stroke:#999
    style CTRL fill:#d5e8d4,stroke:#82b366
    style SVC fill:#fff2cc,stroke:#d6b656
    style PROC fill:#ffe6cc,stroke:#d79b00
    style CONN fill:#f8cecc,stroke:#b85450
    style RPT fill:#d5e8d4,stroke:#82b366
```

---

## 2. Fluxo da Importação

Passo a passo do processamento de cada linha do CSV.

```mermaid
flowchart TD
    A(["📤 Upload do CSV"]) --> B["CsvService\nLê e parseia o arquivo"]
    B --> C{Arquivo\nválido?}
    C -- Não --> D(["❌ HTTP 500\nErro de leitura"])
    C -- Sim --> E[/"Para cada linha do CSV"/]

    E --> F{CPF\ninformado?}
    F -- Não --> G["ERRO\nCPF não informado"]
    F -- Sim --> H{CPF duplicado\nno arquivo?}

    H -- Sim --> I["DUPLICADO\nDuplicado no CSV"]
    H -- Não --> J["ClienteCsvValidator\nValida campos obrigatórios\ne dígitos verificadores"]

    J --> K{Dados\nválidos?}
    K -- Não --> L["ERRO\nErro de validação"]
    K -- Sim --> M["BuniClienteConnector\nConsulta CPF na API B.uni"]

    M --> N{Cliente existe\nna base?}
    N -- Sim --> O["DUPLICADO\nJá cadastrado na base"]
    N -- Não --> P["ClienteMapper\nMonta payload da API\nEnriquece endereço via ViaCEP"]

    P --> Q["BuniClienteConnector\nInclui cliente via API"]
    Q --> R{API retornou\nsucesso?}
    R -- Sim --> S["✅ SUCESSO\nCliente incluído"]
    R -- Não --> T["ERRO\nFalha na API"]

    G & I & L & O & S & T --> U["ExecutionReportLine\nRegistra resultado da linha"]
    U --> V{Mais\nlinhas?}
    V -- Sim --> E
    V -- Não --> W["ImportacaoReportGenerator\nGera relatório CSV em output/"]

    W --> X["Calcula status geral\nSUCESSO / FALHA / PROCESSADO_COM_ALERTAS..."]
    X --> Y["Retorna JSON com resumo\n+ URL de download"]
    Y --> Z(["🖥️ Frontend exibe resultado\ne habilita download"])

    style A fill:#dae8fc,stroke:#6c8ebf
    style D fill:#f8cecc,stroke:#b85450
    style G fill:#f8cecc,stroke:#b85450
    style I fill:#fff2cc,stroke:#d6b656
    style L fill:#f8cecc,stroke:#b85450
    style O fill:#fff2cc,stroke:#d6b656
    style S fill:#d5e8d4,stroke:#82b366
    style T fill:#f8cecc,stroke:#b85450
    style Z fill:#dae8fc,stroke:#6c8ebf
```

---

## 3. Arquitetura em Camadas

Organização vertical das responsabilidades do sistema.

```mermaid
graph TB
    subgraph L0["🌐 Apresentação"]
        FE["Frontend\nHTML / CSS / JS\napp.js · style.css · index.html"]
    end

    subgraph L1["🔌 Controller"]
        IC["ImportacaoClienteController\n POST /importar/clientes\n GET /importar/relatorios/\n GET /health"]
    end

    subgraph L2["⚙️ Service"]
        CIS["ClienteImportService\nOrquestra todo o processamento"]
        CSV["CsvService\nLeitura e parsing do CSV"]
    end

    subgraph L3["🔍 Validação e Mapeamento"]
        VAL["ClienteCsvValidator\nCampos obrigatórios · CPF · E-mail · Renda"]
        MAP["ClienteMapper\nCSV → ClienteRequest\nEnriquecimento de endereço"]
    end

    subgraph L4["🔗 Connector"]
        AUTH["BuniAuthConnector\nOAuth Token com cache"]
        BUNI["BuniClienteConnector\nConsulta CPF · Inclui Cliente"]
        CEP["CepConnector\nViaCEP — Endereço por CEP"]
    end

    subgraph L5["📄 Relatório"]
        RG["ImportacaoReportGenerator\nGera CSV detalhado por linha"]
    end

    subgraph L6["🌍 APIs Externas"]
        AUTHAPI["API B.uni — OAuth"]
        CLIAPI["API B.uni — Cliente PF"]
        VCEP["API ViaCEP"]
    end

    FE -->|"HTTP multipart"| IC
    IC --> CIS
    IC --> CSV
    CIS --> VAL
    CIS --> MAP
    CIS --> BUNI
    CIS --> RG
    MAP --> CEP
    BUNI --> AUTH
    AUTH --> AUTHAPI
    BUNI --> CLIAPI
    CEP --> VCEP

    style L0 fill:#dae8fc,stroke:#6c8ebf
    style L1 fill:#d5e8d4,stroke:#82b366
    style L2 fill:#fff2cc,stroke:#d6b656
    style L3 fill:#ffe6cc,stroke:#d79b00
    style L4 fill:#f8cecc,stroke:#b85450
    style L5 fill:#d5e8d4,stroke:#82b366
    style L6 fill:#e1d5e7,stroke:#9673a6
```

---

## 4. Fluxo de Integração REST

Todas as chamadas HTTP realizadas entre os componentes e as APIs externas.

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant FE as Frontend
    participant IC as ImportacaoClienteController
    participant CIS as ClienteImportService
    participant AUTH as BuniAuthConnector
    participant BUNI as BuniClienteConnector
    participant CEP as CepConnector
    participant AUTHAPI as API B.uni OAuth
    participant CLIAPI as API B.uni Cliente PF
    participant VCEP as API ViaCEP

    U->>FE: Seleciona CSV e clica em Processar
    FE->>IC: POST /importar/clientes (multipart/form-data)

    loop Para cada cliente no CSV
        CIS->>AUTH: gerarToken()
        AUTH->>AUTHAPI: POST /oauth/token (form-urlencoded)
        Note right of AUTHAPI: client_id, client_secret,<br/>grant_type, username, password
        AUTHAPI-->>AUTH: { access_token, expires_in }
        AUTH-->>CIS: Bearer Token (cacheado)

        CIS->>BUNI: clienteExistePorCpf(cpf)
        BUNI->>CLIAPI: GET /ClientePorCPF?TipoConsulta=0&CPF={cpf}
        alt CPF encontrado (HTTP 200)
            CLIAPI-->>BUNI: { Clientes: [...] }
            BUNI-->>CIS: true → DUPLICADO
        else CPF não encontrado (HTTP 404 ou código 100000026)
            CLIAPI-->>BUNI: 404 / { CODIGO: 100000026 }
            BUNI-->>CIS: false → segue para inclusão

            CIS->>CEP: buscarCep(cep)
            CEP->>VCEP: GET /ws/{cep}/json/
            alt CEP encontrado
                VCEP-->>CEP: { logradouro, bairro, localidade, uf }
                CEP-->>CIS: CepResponse com dados
            else CEP não encontrado
                VCEP-->>CEP: { erro: true } ou timeout
                CEP-->>CIS: null → fallback aplicado (UF=SP)
            end

            CIS->>BUNI: incluirCliente(ClienteRequest)
            BUNI->>CLIAPI: POST /ClientePF_IncluirCliente (JSON)
            alt Inclusão bem-sucedida
                CLIAPI-->>BUNI: { CodigoCliente, STATUS }
                BUNI-->>CIS: InclusaoClienteResponse → SUCESSO
            else Erro na API
                CLIAPI-->>BUNI: HTTP 4xx/5xx
                BUNI-->>CIS: RuntimeException [HTTP XXX] → ERRO
            end
        end
    end

    CIS-->>IC: ProcessamentoResult + Path do relatório
    IC-->>FE: JSON { status, totalSucesso, totalErro, totalDuplicado, downloadUrl }
    FE-->>U: Exibe resumo e habilita download

    U->>FE: Clica em Baixar Relatório
    FE->>IC: GET /importar/relatorios/{nomeArquivo}
    IC-->>FE: CSV binário (Content-Disposition: attachment)
    FE-->>U: Download do arquivo CSV
```

---

## 5. Fluxo de Tratamento de Erros

Como o sistema lida com cada tipo de falha.

```mermaid
flowchart TD
    subgraph ENTRADA["Tipos de Falha na Entrada"]
        E1["CSV com separador\nerrado (vírgula)"]
        E2["CSV com encoding\nerrado (ISO-8859-1)"]
        E3["Arquivo não-CSV\nou corrompido"]
    end

    subgraph VALIDACAO["Erros de Validação (por linha)"]
        V1["CPF vazio\nou ausente"]
        V2["CPF com dígitos\nverificadores inválidos"]
        V3["Campo obrigatório\nausente"]
        V4["E-mail sem @\nou ausente"]
        V5["Salário zero\nou inválido"]
    end

    subgraph INTEGRACAO["Erros de Integração"]
        I1["Timeout na API B.uni\n(10s conexão / 30s leitura)"]
        I2["HTTP 4xx da API B.uni\n(payload inválido)"]
        I3["HTTP 5xx da API B.uni\n(instabilidade)"]
        I4["Falha no ViaCEP\n(timeout ou CEP inválido)"]
    end

    E1 & E2 & E3 --> ER1(["❌ RuntimeException\nHTTP 500 para o frontend\nProcessamento abortado"])

    V1 --> R1["ERRO — CPF não informado"]
    V2 --> R2["ERRO — CPF inválido\ndígitos verificadores incorretos"]
    V3 --> R3["ERRO — Campo obrigatório ausente"]
    V4 --> R4["ERRO — E-mail inválido"]
    V5 --> R5["ERRO — Salário inválido"]

    I1 --> R6["ERRO — Falha técnica\ncódigo HTTP 0"]
    I2 --> R7["ERRO — Erro da API\ncódigo HTTP 4xx real"]
    I3 --> R8["ERRO — Erro da API\ncódigo HTTP 5xx real"]
    I4 --> FB["⚠️ Fallback aplicado\nBairro/Cidade = NAO INFORMADO\nUF = SP\nProcessamento continua"]

    R1 & R2 & R3 & R4 & R5 & R6 & R7 & R8 & FB --> REL["ExecutionReportLine\nRegistra status ERRO\ncom MotivoErro e AcaoSugerida"]

    REL --> CSV["Relatório CSV\ncom detalhe por linha"]
    CSV --> DASH["Dashboard Frontend\ntotalErro incrementado"]
    DASH --> DL(["📥 Download do Relatório\npara análise"])

    style ER1 fill:#f8cecc,stroke:#b85450
    style FB fill:#fff2cc,stroke:#d6b656
    style DL fill:#d5e8d4,stroke:#82b366
```

---

## 6. Estrutura do Projeto

Organização dos pacotes e suas responsabilidades.

```mermaid
graph LR
    ROOT["integration-engine"] --> SRC["src/main/java\nbr.com.buni.integration.core"]
    ROOT --> RES["src/main/resources"]
    ROOT --> OUT["output/"]
    ROOT --> LOGS["logs/"]
    ROOT --> INP["input/"]

    SRC --> CTR["controller/\nImportacaoClienteController\nHealthController"]
    SRC --> SVC["service/\nClienteImportService\nCsvService"]
    SRC --> CON["connector/\nBuniAuthConnector\nBuniClienteConnector\nCepConnector"]
    SRC --> MAP["mapper/\nClienteMapper"]
    SRC --> VAL["validator/\nClienteCsvValidator"]
    SRC --> RPT["report/\nImportacaoReportGenerator"]
    SRC --> LOG["logging/\nLogService"]
    SRC --> CFG["config/\nRestTemplateConfig"]
    SRC --> MOD["model/"]
    SRC --> ENM["enums/\nSexoEnum · EscolaridadeEnum\nEstadoCivilEnum · NacionalidadeEnum\nTipoDocumentoEnum · TipoResidenciaEnum\nTipoTelefoneEnum · TipoConsultaClienteEnum"]
    SRC --> EXC["exception/\nIntegrationException"]
    SRC --> UTL["util/\nStringUtils · DateUtils · FileUtils"]

    MOD --> MCSV["csv/\nClienteCsv"]
    MOD --> MREQ["request/\nClienteRequest"]
    MOD --> MRES["response/\nTokenResponse · CepResponse\nInclusaoClienteResponse\nConsultaClienteCpfResponse"]
    MOD --> MDTO["dto/\nExecutionReportLine\nExecutionReportSummary\nProcessamentoResult"]

    RES --> PROP["application.properties"]
    RES --> LBK["logback-spring.xml"]
    RES --> STA["static/\nindex.html · style.css · app.js"]

    style ROOT fill:#005E91,color:#fff,stroke:#003d5c
    style SRC fill:#d5e8d4,stroke:#82b366
    style RES fill:#dae8fc,stroke:#6c8ebf
    style MOD fill:#fff2cc,stroke:#d6b656
    style CON fill:#f8cecc,stroke:#b85450
    style ENM fill:#e1d5e7,stroke:#9673a6
```

---

## 7. Fluxo de Classes — Quem Chama Quem

Interações entre as classes durante o processamento.

```mermaid
graph TD
    FE["🌐 Frontend\napp.js"]
    IC["ImportacaoClienteController"]
    CSV["CsvService"]
    CIS["ClienteImportService"]
    LOG["LogService"]
    VAL["ClienteCsvValidator"]
    MAP["ClienteMapper"]
    BUNI["BuniClienteConnector"]
    AUTH["BuniAuthConnector"]
    CEP["CepConnector"]
    RG["ImportacaoReportGenerator"]

    SU["StringUtils"]
    DU["DateUtils"]

    AUTHAPI(["API B.uni\nOAuth"])
    CLIAPI(["API B.uni\nCliente PF"])
    VCEP(["API ViaCEP"])

    ERL["ExecutionReportLine\n(DTO por linha)"]
    PR["ProcessamentoResult\n(DTO de retorno)"]

    FE -->|"POST multipart"| IC
    IC -->|"lerClientes(file)"| CSV
    IC -->|"processar(nome, clientes)"| CIS

    CIS -->|"validar(cliente)"| VAL
    CIS -->|"toRequest(csv)"| MAP
    CIS -->|"clienteExistePorCpf(cpf)"| BUNI
    CIS -->|"incluirCliente(request)"| BUNI
    CIS -->|"gerarRelatorioCsv(linhas)"| RG
    CIS -->|"log eventos"| LOG
    CIS -->|"monta linhas"| ERL
    CIS -->|"retorna"| PR

    MAP -->|"buscarCep(cep)"| CEP
    MAP -->|"normalizarCpf / limparNumero"| SU
    MAP -->|"formatarDataBrParaIso"| DU

    BUNI -->|"gerarToken()"| AUTH
    AUTH -->|"POST /oauth/token"| AUTHAPI
    BUNI -->|"GET /ClientePorCPF"| CLIAPI
    BUNI -->|"POST /IncluirCliente"| CLIAPI
    CEP -->|"GET /ws/{cep}/json/"| VCEP

    VAL -->|"normalizarCpf / vazio"| SU

    IC -->|"JSON response"| FE

    style FE fill:#dae8fc,stroke:#6c8ebf
    style IC fill:#d5e8d4,stroke:#82b366
    style CIS fill:#fff2cc,stroke:#d6b656
    style CSV fill:#fff2cc,stroke:#d6b656
    style VAL fill:#ffe6cc,stroke:#d79b00
    style MAP fill:#ffe6cc,stroke:#d79b00
    style BUNI fill:#f8cecc,stroke:#b85450
    style AUTH fill:#f8cecc,stroke:#b85450
    style CEP fill:#f8cecc,stroke:#b85450
    style RG fill:#d5e8d4,stroke:#82b366
    style AUTHAPI fill:#e1d5e7,stroke:#9673a6
    style CLIAPI fill:#e1d5e7,stroke:#9673a6
    style VCEP fill:#e1d5e7,stroke:#9673a6
    style ERL fill:#f5f5f5,stroke:#999
    style PR fill:#f5f5f5,stroke:#999
```

---

## 8. Fluxo do Relatório

Como o relatório é construído, gerado e disponibilizado.

```mermaid
flowchart LR
    subgraph PROC["Processamento por Linha"]
        L1["Linha processada\ncom SUCESSO"]
        L2["Linha com\nERRO de validação"]
        L3["Linha com\nERRO de API"]
        L4["Linha DUPLICADA\n(arquivo ou base)"]
    end

    subgraph DTO["Montagem do DTO"]
        ERL["ExecutionReportLine\n─────────────────\ndataHoraExecucao\nnomeArquivo · linhaCsv\nimportId · correlationId\ncpf · nome · status\ncodigoCliente · codigoHttp\ncampoErro · valorRecebido\nmotivoErro · acaoSugerida\nmensagemApi · fallbackAplicado\ntempoProcessamentoMs · observacao"]
    end

    subgraph GEN["Geração do Arquivo"]
        RG["ImportacaoReportGenerator\ngerarRelatorioCsv()"]
        FILE[("output/\nrelatorio_TIMESTAMP_arquivo.csv")]
    end

    subgraph RES["Resultado"]
        PR["ProcessamentoResult\nstatus · totalSucesso\ntotalErro · totalDuplicado\ntempoTotalMs · caminhoRelatorio"]
        JSON["JSON Response\n{ status, totalLinhas,\ntotalSucesso, totalErro,\ntotalDuplicado, downloadUrl }"]
        DASH["Dashboard Frontend\nExibe resumo"]
        DL(["📥 GET /importar/relatorios/\n{nomeArquivo}\nDownload CSV"])
    end

    L1 -->|"buildSuccessLine()"| ERL
    L2 -->|"buildValidationErrorLine()"| ERL
    L3 -->|"buildApiErrorLine()"| ERL
    L4 -->|"buildBaseDuplicateLine()\nbuildFileDuplicateLine()"| ERL

    ERL -->|"List<ExecutionReportLine>"| RG
    RG --> FILE

    FILE --> PR
    PR --> JSON
    JSON --> DASH
    DASH --> DL

    style L1 fill:#d5e8d4,stroke:#82b366
    style L2 fill:#f8cecc,stroke:#b85450
    style L3 fill:#f8cecc,stroke:#b85450
    style L4 fill:#fff2cc,stroke:#d6b656
    style ERL fill:#f5f5f5,stroke:#999
    style RG fill:#d5e8d4,stroke:#82b366
    style FILE fill:#f5f5f5,stroke:#666
    style PR fill:#fff2cc,stroke:#d6b656
    style DASH fill:#dae8fc,stroke:#6c8ebf
    style DL fill:#dae8fc,stroke:#6c8ebf
```

---

> **Legenda de Cores**
>
> | Cor | Camada |
> |-----|--------|
> | 🔵 Azul | Frontend / Usuário |
> | 🟢 Verde | Controller / Service / Relatório |
> | 🟡 Amarelo | Service / DTO |
> | 🟠 Laranja | Validação / Mapeamento |
> | 🔴 Vermelho | Connectors |
> | 🟣 Roxo | APIs Externas |
> | ⚪ Cinza | Utilitários / Armazenamento |
