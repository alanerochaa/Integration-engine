package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExecutionReportSummary {

    private String nomeArquivo;
    private LocalDateTime inicioExecucao;
    private LocalDateTime fimExecucao;

    private int totalLinhas;
    private int totalSucesso;
    private int totalErro;

    private List<ExecutionReportLine> linhas;
}