package br.com.buni.integration.core.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionReportLine {

    /*
     Data/hora exata da execução da linha
    */
    private LocalDateTime dataHoraExecucao;

    /*
     Arquivo original enviado
    */
    private String nomeArquivo;

    /*
     Número da linha do CSV
    */
    private Integer linhaCsv;

    /*
     ID do lote
     Ex:
     IMPORT-20260624-164213
    */
    private String importId;

    /*
     ID único da linha
     Ex:
     IMPORT-20260624-164213-L00015
    */
    private String correlationId;

    /*
     Ambiente executado
     HML / PROD
    */
    private String ambiente;

    private String cpf;

    private String nome;

    /*
     SUCESSO
     ERRO
     DUPLICADO
     IGNORADO
    */
    private String status;

    /*
     Código retornado pela API
     usado para consultar no sistema depois
    */
    private String codigoCliente;

    /*
     HTTP retorno
     200
     201
     400
     500
    */
    private Integer codigoHttp;

    /*
     Campo inválido
    */
    private String campoErro;

    /*
     Valor recebido no CSV
    */
    private String valorRecebido;

    /*
     Motivo amigável
    */
    private String motivoErro;

    /*
     Próxima ação sugerida
    */
    private String acaoSugerida;

    /*
     protocolo externo
    */
    private String protocoloApi;

    /*
     mensagem API
    */
    private String mensagemApi;

    /*
     fallback endereço
    */
    private Boolean fallbackAplicado;

    /*
     tempo da linha
    */
    private Long tempoProcessamentoMs;

    /*
     observação final
    */
    private String observacao;
}