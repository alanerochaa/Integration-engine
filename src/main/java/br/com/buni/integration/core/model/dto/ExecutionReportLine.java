package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionReportLine {

    private LocalDateTime dataHoraExecucao;

    private String nomeArquivo;

    private Integer linhaCsv;

    private String importId;

    private String correlationId;

    private String ambiente;

    private String cpf;

    private String nome;

    private String status;

    private String codigoCliente;

    private Integer codigoHttp;

    private String campoErro;

    private String valorRecebido;

    private String motivoErro;

    private String acaoSugerida;

    private String protocoloApi;

    private String mensagemApi;

    private Boolean fallbackAplicado;

    private Long tempoProcessamentoMs;

    private String observacao;
}