package br.com.buni.integration.core.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InclusaoClienteResponse {

    @JsonProperty("CodigoCliente")
    private String codigoCliente;

    @JsonProperty("STATUS")
    private StatusApi status;

    @Data
    public static class StatusApi {

        @JsonProperty("CODIGO")
        private String codigo;

        @JsonProperty("ERROS")
        private Erros erros;
    }

    @Data
    public static class Erros {

        @JsonProperty("ERRO")
        private List<String> erro;
    }

    public String getMensagem() {

        if (status == null) {
            return "";
        }

        if (
                status.getErros() == null
                        || status.getErros().getErro() == null
                        || status.getErros().getErro().isEmpty()
        ) {
            return status.getCodigo();
        }

        return String.join(
                " | ",
                status.getErros().getErro()
        );
    }

    public String getProtocolo() {
        return codigoCliente;
    }
}