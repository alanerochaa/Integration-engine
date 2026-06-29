package br.com.buni.integration.core.service;

import br.com.buni.integration.core.connector.BuniClienteConnector;
import br.com.buni.integration.core.logging.LogService;
import br.com.buni.integration.core.mapper.ClienteMapper;
import br.com.buni.integration.core.model.csv.ClienteCsv;
import br.com.buni.integration.core.model.dto.ExecutionReportLine;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.model.request.ClienteRequest;
import br.com.buni.integration.core.model.response.InclusaoClienteResponse;
import br.com.buni.integration.core.report.ImportacaoReportGenerator;
import br.com.buni.integration.core.util.StringUtils;
import br.com.buni.integration.core.validator.ClienteCsvValidator;
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
public class ClienteImportService {

    private final ClienteCsvValidator validator;
    private final ClienteMapper mapper;
    private final BuniClienteConnector clienteConnector;
    private final ImportacaoReportGenerator reportService;
    private final LogService logService;

    @Value("${buni.ambiente:HML}")
    private String ambiente;

    /**
     * Processa um lote de clientes a partir de um arquivo CSV.
     * Todos os contadores são locais a cada chamada — seguro para requisições concorrentes (sem estado compartilhado).
     */
    public ProcessamentoResult processar(String nomeArquivo, List<ClienteCsv> clientes) {

        LocalDateTime start = LocalDateTime.now();
        String importId = buildImportId(start);

        long totalSucesso = 0;
        long totalErro = 0;
        long totalDuplicado = 0;

        Set<String> processedCpfs = new HashSet<>();
        List<ExecutionReportLine> report = new ArrayList<>();

        logService.inicioProcessamento(nomeArquivo, clientes.size());

        for (int i = 0; i < clientes.size(); i++) {

            ClienteCsv cliente = clientes.get(i);
            int linhaCsv = i + 2; // cabeçalho é linha 1, dados começam na linha 2
            String correlationId = buildCorrelationId(importId, linhaCsv);
            String cpfNormalizado = StringUtils.normalizarCpf(cliente.getCpf());
            LocalDateTime lineStart = LocalDateTime.now();

            try {

                if (cpfNormalizado == null || cpfNormalizado.isBlank()) {
                    totalErro++;
                    report.add(buildValidationErrorLine(nomeArquivo, linhaCsv, cliente,
                            List.of("CPF nao informado"), importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                if (processedCpfs.contains(cpfNormalizado)) {
                    totalDuplicado++;
                    logService.erroValidacao(linhaCsv, cpfNormalizado, "CPF duplicado no arquivo. Registro ignorado.");
                    report.add(buildFileDuplicateLine(nomeArquivo, linhaCsv, cliente,
                            importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                processedCpfs.add(cpfNormalizado);

                List<String> validationErrors = validator.validar(cliente);
                if (!validationErrors.isEmpty()) {
                    totalErro++;
                    logService.erroValidacao(linhaCsv, cpfNormalizado, String.join(" | ", validationErrors));
                    report.add(buildValidationErrorLine(nomeArquivo, linhaCsv, cliente,
                            validationErrors, importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                // CPF não encontrado na base — segue para inclusão (código 100000026 tratado no connector)
                boolean customerExists = clienteConnector.clienteExistePorCpf(cpfNormalizado);

                if (customerExists) {
                    totalDuplicado++;
                    logService.erroValidacao(linhaCsv, cpfNormalizado, "CPF ja cadastrado na base. Inclusao ignorada.");
                    report.add(buildBaseDuplicateLine(nomeArquivo, linhaCsv, cliente,
                            importId, correlationId, elapsed(lineStart)));
                    continue;
                }

                ClienteRequest request = mapper.toRequest(cliente);
                InclusaoClienteResponse response = clienteConnector.incluirCliente(request);

                totalSucesso++;
                logService.sucessoLinha(linhaCsv, cpfNormalizado, cliente.getNome());
                report.add(buildSuccessLine(nomeArquivo, linhaCsv, cliente,
                        response, importId, correlationId, elapsed(lineStart)));

            } catch (Exception e) {
                totalErro++;
                logService.erroApi(linhaCsv, cpfNormalizado, e.getMessage());
                report.add(buildApiErrorLine(nomeArquivo, linhaCsv, cliente,
                        e, importId, correlationId, elapsed(lineStart)));
            }
        }

        long totalMs = Duration.between(start, LocalDateTime.now()).toMillis();
        String statusGeral = resolveOverallStatus(totalSucesso, totalErro, totalDuplicado);

        Path reportPath = reportService.gerarRelatorioCsv(nomeArquivo, report);
        logService.fimProcessamento(nomeArquivo, reportPath.toString());

        return ProcessamentoResult.builder()
                .statusGeral(statusGeral)
                .totalSucesso(totalSucesso)
                .totalErro(totalErro)
                .totalDuplicado(totalDuplicado)
                .tempoTotalMs(totalMs)
                .caminhoRelatorio(reportPath)
                .build();
    }

    private ExecutionReportLine buildSuccessLine(String arquivo, Integer linha, ClienteCsv cliente,
            InclusaoClienteResponse response, String importId, String correlationId, long tempoMs) {
        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(cliente.getCpf())).nome(cliente.getNome())
                .status("SUCESSO")
                .codigoCliente(response != null ? response.getCodigoCliente() : "")
                .codigoHttp(200).mensagemApi("Cliente incluido com sucesso")
                .fallbackAplicado(Boolean.TRUE.equals(cliente.getEnderecoFallbackAplicado()))
                .tempoProcessamentoMs(tempoMs).observacao(cliente.getObservacaoEndereco())
                .build();
    }

    private ExecutionReportLine buildBaseDuplicateLine(String arquivo, Integer linha, ClienteCsv cliente,
            String importId, String correlationId, long tempoMs) {
        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(cliente.getCpf())).nome(cliente.getNome())
                .status("DUPLICADO").codigoCliente("").codigoHttp(200)
                .mensagemApi("CPF ja cadastrado na base. Inclusao ignorada.")
                .motivoErro("Cliente ja existente")
                .acaoSugerida("Nao reenviar este CPF para inclusao")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs).observacao(cliente.getObservacaoEndereco())
                .build();
    }

    private ExecutionReportLine buildFileDuplicateLine(String arquivo, Integer linha, ClienteCsv cliente,
            String importId, String correlationId, long tempoMs) {
        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(cliente.getCpf())).nome(cliente.getNome())
                .status("DUPLICADO").codigoHttp(0)
                .mensagemApi("CPF duplicado no proprio arquivo. Inclusao ignorada.")
                .motivoErro("CPF duplicado no CSV")
                .acaoSugerida("Manter apenas uma ocorrencia do CPF no arquivo")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs).observacao(cliente.getObservacaoEndereco())
                .build();
    }

    private ExecutionReportLine buildValidationErrorLine(String arquivo, Integer linha, ClienteCsv cliente,
            List<String> erros, String importId, String correlationId, long tempoMs) {
        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(cliente.getCpf())).nome(cliente.getNome())
                .status("ERRO").codigoHttp(0)
                .mensagemApi("Erro de validacao no CSV")
                .motivoErro(String.join(" | ", erros))
                .acaoSugerida("Corrigir dados de entrada no CSV")
                .fallbackAplicado(false)
                .tempoProcessamentoMs(tempoMs).observacao(cliente.getObservacaoEndereco())
                .build();
    }

    private ExecutionReportLine buildApiErrorLine(String arquivo, Integer linha, ClienteCsv cliente,
            Exception e, String importId, String correlationId, long tempoMs) {
        return ExecutionReportLine.builder()
                .dataHoraExecucao(LocalDateTime.now())
                .nomeArquivo(arquivo).linhaCsv(linha)
                .importId(importId).correlationId(correlationId).ambiente(ambiente)
                .cpf(StringUtils.normalizarCpf(cliente.getCpf())).nome(cliente.getNome())
                .status("ERRO")
                .codigoHttp(extractHttpCode(e.getMessage()))
                .mensagemApi("Erro no processamento da API")
                .motivoErro(e.getMessage())
                .acaoSugerida("Validar retorno da API e payload enviado")
                .fallbackAplicado(Boolean.TRUE.equals(cliente.getEnderecoFallbackAplicado()))
                .tempoProcessamentoMs(tempoMs).observacao(cliente.getObservacaoEndereco())
                .build();
    }

    /** Extrai o código HTTP real da mensagem de exceção do connector, no formato "[HTTP 4xx] ..." */
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

    private String buildImportId(LocalDateTime dateTime) {
        return "IMPORT-" + dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String buildCorrelationId(String importId, int line) {
        return importId + "-L" + String.format("%05d", line);
    }

    private long elapsed(LocalDateTime start) {
        return Duration.between(start, LocalDateTime.now()).toMillis();
    }
}
