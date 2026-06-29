package br.com.buni.integration.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsultaClienteCpfResponse {

    @JsonProperty("CPF")
    private String cpf;

    @JsonProperty("Clientes")
    private List<ClienteEncontrado> clientes;

    @JsonProperty("STATUS")
    private StatusApi status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClienteEncontrado {

        @JsonProperty("CodigoCliente")
        private String codigoCliente;

        @JsonProperty("Codigo")
        private String codigo;

        @JsonProperty("Nome")
        private String nome;

        @JsonProperty("CPF")
        private String cpf;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusApi {

        @JsonProperty("CODIGO")
        private String codigo;
    }

    public boolean possuiCliente() {
        return clientes != null && !clientes.isEmpty();
    }

    public String primeiroCodigoCliente() {

        if (!possuiCliente()) {
            return null;
        }

        ClienteEncontrado cliente = clientes.get(0);

        if (
                cliente.getCodigoCliente() != null
                        && !cliente.getCodigoCliente().isBlank()
        ) {
            return cliente.getCodigoCliente();
        }

        return cliente.getCodigo();
    }
}