package br.com.buni.integration.core.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Resposta da API B.uni — CadastrarFuncionarioEXT.
 * Estrutura definida com base no contrato da API FIAPIEmprCadastroFunc.
 * Os campos @JsonProperty devem ser validados após homologação com a API real.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CadastroFuncionarioResponse {

    @JsonProperty("CodigoFuncionario")
    private String codigoFuncionario;

    @JsonProperty("Status")
    private StatusApi status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusApi {

        @JsonProperty("Codigo")
        private String codigo;

        @JsonProperty("Mensagem")
        private String mensagem;
    }
}
