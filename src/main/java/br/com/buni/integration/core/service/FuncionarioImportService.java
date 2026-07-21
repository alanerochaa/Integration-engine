package br.com.buni.integration.core.service;

import br.com.buni.integration.core.connector.BuniFuncionarioConnector;
import br.com.buni.integration.core.logging.LogService;
import br.com.buni.integration.core.mapper.FuncionarioMapper;
import br.com.buni.integration.core.model.dto.ExecutionReportLine;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import br.com.buni.integration.core.model.request.FuncionarioRequest;
import br.com.buni.integration.core.model.response.CadastroFuncionarioResponse;
import br.com.buni.integration.core.report.ExcelReportGenerator;
import br.com.buni.integration.core.report.ImportacaoReportGenerator;
import br.com.buni.integration.core.util.StringUtils;
import br.com.buni.integration.core.validator.FuncionarioValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FuncionarioImportService implements ImportProcessor<FuncionarioImportRow> {

    private final FuncionarioValidator validator;
    private final FuncionarioMapper mapper;
    private final BuniFuncionarioConnector funcionarioConnector;
    private final ImportacaoReportGenerator reportService;
    private final ExcelReportGenerator excelReportGenerator;
    private final LogService logService;
    private final FuncionarioCpfRegistry cpfRegistry;

    @Value("${buni.ambiente:HML}")
    private String ambiente;

    @Override
    public ProcessamentoResult processar(String nomeArquivo, List<FuncionarioImportRow> funcionarios) {

        LocalDateTime start   = LocalDateTime.now();
        String importId       = buildImportId(start);

        long totalSucesso    = 0;
        long totalErro       = 0;
        long totalDuplicado  = 0;

        Set<String> cpfsProcessados = new HashSet<>();
        List<ExecutionReportLine> report = new ArrayList<>();

        logService.inicioProcessamento(nomeArquivo, funcionarios.size());

        for (int i = 0; i < funcionarios.size(); i++) {

            FuncionarioImportRow row = funcionarios.get(i);
            int linha           = i + 2;
            String correlationId = buildCorrelationId(importId, linha);
            String cpf           = StringUtils.normalizarCpf(row.getCpf());
            LocalDateTime lineStart = LocalDateTime.now();

            try {

                if (cpf == null || cpf.isBlank()) {
                    totalErro++;
                    report.add(buildValidationErrorLine(nomeArquivo, linha, row,
                            List.of("CPF nao informado"), importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                // ── 1. Duplicidade dentro do mesmo arquivo ─────────────────────────
                if (cpfsProcessados.contains(cpf)) {
                    totalDuplicado++;
                    logService.erroValidacao(linha, cpf, "CPF duplicado no arquivo. Registro ignorado.");
                    report.add(buildFileDuplicateLine(nomeArquivo, linha, row,
                            importId, correlationId, elapsed(lineStart)));
                    continue;
                }
                cpfsProcessados.add(cpf);

                // ── 2. Reserva atômica do CPF (verifica + registra numa só operação) ─
                boolean reservado = cpfRegistry.registrar(cpf, importId);
                if (!reservado) {
                    totalDuplicado++;
                    String origemImportId = cpfRegistry.importIdOrigem(cpf);
                    logService.erroValidacao(linha, cpf,
                            "CPF ja cadastrado (importId de origem: " + origemImportId + "). Registro ignorado.");
                    report.add(buildAlreadyRegisteredLine(nomeArquivo, linha, row,
                            origemImportId, importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                // ── 3. Validação dos dados ──────────────────────────────────────────
                List<String> validationErrors = validator.validar(row);
                if (!validationErrors.isEmpty()) {
                    cpfRegistry.liberarRegistro(cpf); // libera para permitir reenvio corrigido
                    totalErro++;
                    logService.erroValidacao(linha, cpf, String.join(" | ", validationErrors));
                    report.add(buildValidationErrorLine(nomeArquivo, linha, row,
                            validationErrors, importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                // ── 4. Chamada à API ────────────────────────────────────────────────
                FuncionarioRequest request = mapper.toRequest(row);
                CadastroFuncionarioResponse response = funcionarioConnector.cadastrarFuncionario(request);

                totalSucesso++;
                logService.sucessoLinha(linha, cpf, row.getNomeFuncionario());
                report.add(buildSuccessLine(nomeArquivo, linha, row,
                        response, importId, correlationId, elapsed(lineStart)));

            } catch (Exception e) {
                cpfRegistry.liberarRegistro(cpf); // libera para permitir reenvio após erro de API
                totalErro++;
                logService.erroApi(linha, cpf, e.getMessage());
                report.add(buildApiErrorLine(nomeArquivo, linha, row,
                        e, importId, correlationId, elapsed(lineStart)));
            }
        }

        long totalMs    = Duration.between(start, LocalDateTime.now()).toMillis();
        String statusGeral = resolveOverallStatus(totalSucesso, totalErro, totalDuplicado);

        Path reportPath = reportService.gerarRelatorio(nomeArquivo, report);
        Path excelPath  = excelReportGenerator.gerarExcel(nomeArquivo, report);
        logService.fimProcessamento(nomeArquivo, reportPath.toString(), totalMs, totalSucesso, totalErro, totalDuplicado);

        return ProcessamentoResult.builder()
                .importId(importId)
                .statusGeral(statusGeral)
                .totalSucesso(totalSucesso)
                .totalErro(totalErro)
                .totalDuplicado(totalDuplicado)
                .tempoTotalMs(totalMs)
                .caminhoRelatorio(reportPath)
                .caminhoExcel(excelPath)
                .build();
    }

    // ─── Builders de linha de relatório ──────────────────────────────────────────

    private ExecutionReportLine buildSuccessLine(String arquivo, Integer linha,
            FuncionarioImportRow row, CadastroFuncionarioResponse response,
            String importId, String correlationId, long tempoMs) {

        String codigoRetornado = (response != null && response.getCodigoFuncionario() != null)
                ? response.getCodigoFuncionario()
                : "";

        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(row.getCpf())).nome(row.getNomeFuncionario())
                .status("SUCESSO")
                .codigoCliente(codigoRetornado)
                .codigoHttp(200)
                .mensagemApi("Funcionario cadastrado com sucesso")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs)
                .observacao(row.getObservacao())
                .build();
    }

    private ExecutionReportLine buildAlreadyRegisteredLine(String arquivo, Integer linha,
            FuncionarioImportRow row, String origemImportId,
            String importId, String correlationId, long tempoMs) {

        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(row.getCpf())).nome(row.getNomeFuncionario())
                .status("JA_CADASTRADO").codigoHttp(0)
                .mensagemApi("CPF já cadastrado. Inclusão bloqueada.")
                .motivoErro("CPF já cadastrado anteriormente (importId de origem: " + origemImportId + ")")
                .acaoSugerida("Remova este CPF do arquivo. Para atualizar o cadastro, utilize o endpoint de atualização.")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs)
                .observacao(row.getObservacao())
                .build();
    }

    private ExecutionReportLine buildFileDuplicateLine(String arquivo, Integer linha,
            FuncionarioImportRow row, String importId, String correlationId, long tempoMs) {

        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(row.getCpf())).nome(row.getNomeFuncionario())
                .status("DUPLICADO").codigoHttp(0)
                .mensagemApi("CPF duplicado no proprio arquivo. Cadastro ignorado.")
                .motivoErro("CPF duplicado no Excel")
                .acaoSugerida("Manter apenas uma ocorrencia do CPF no arquivo")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs)
                .observacao(row.getObservacao())
                .build();
    }

    private ExecutionReportLine buildValidationErrorLine(String arquivo, Integer linha,
            FuncionarioImportRow row, List<String> erros,
            String importId, String correlationId, long tempoMs) {

        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(row.getCpf())).nome(row.getNomeFuncionario())
                .status("ERRO").codigoHttp(0)
                .mensagemApi("Erro de validacao no Excel")
                .motivoErro(String.join(" | ", erros))
                .acaoSugerida("Corrigir os dados no Excel e reenviar o arquivo")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs)
                .observacao(row.getObservacao())
                .build();
    }

    private ExecutionReportLine buildApiErrorLine(String arquivo, Integer linha,
            FuncionarioImportRow row, Exception e,
            String importId, String correlationId, long tempoMs) {

        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(row.getCpf())).nome(row.getNomeFuncionario())
                .status("ERRO")
                .codigoHttp(extractHttpCode(e.getMessage()))
                .mensagemApi("Erro no processamento da API")
                .motivoErro(e.getMessage())
                .acaoSugerida("Validar retorno da API e payload enviado")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs)
                .observacao(row.getObservacao())
                .build();
    }

    // ─── Utilitários ─────────────────────────────────────────────────────────────

    private int extractHttpCode(String message) {
        if (message != null && message.startsWith("[HTTP ")) {
            try {
                return Integer.parseInt(message.substring(6, 9));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String resolveOverallStatus(long sucesso, long erro, long duplicado) {
        if (sucesso > 0 && erro == 0 && duplicado == 0) return "SUCESSO";
        if (sucesso > 0 && (erro > 0 || duplicado > 0))  return "PROCESSADO_COM_ALERTAS";
        if (sucesso == 0 && erro > 0 && duplicado == 0)  return "FALHA";
        if (sucesso == 0 && duplicado > 0 && erro == 0)  return "PROCESSADO_COM_DUPLICIDADES";
        if (sucesso == 0 && erro > 0 && duplicado > 0)   return "FALHA_COM_DUPLICIDADES";
        return "SEM_REGISTROS_PROCESSADOS";
    }

    private String buildImportId(LocalDateTime dt) {
        return "IMPORT-FUNC-" + dt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String buildCorrelationId(String importId, int linha) {
        return importId + "-L" + String.format("%05d", linha);
    }

    private long elapsed(LocalDateTime start) {
        return Duration.between(start, LocalDateTime.now()).toMillis();
    }
}
