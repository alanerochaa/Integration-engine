package br.com.buni.integration.core.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuncionarioRequest {

    @JsonProperty("DadosPessoais")
    private DadosPessoais dadosPessoais;

    @JsonProperty("Contato")
    private Contato contato;

    @JsonProperty("DadosEmpresa")
    private DadosEmpresa dadosEmpresa;

    @JsonProperty("DadosBancarios")
    private DadosBancarios dadosBancarios;

    @JsonProperty("RendasMargem")
    private List<RendaMargem> rendasMargem;

    @JsonProperty("Origem4")
    private Origem4 origem4;

    @JsonProperty("Origem5")
    private Origem5 origem5;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DadosPessoais {

        @JsonProperty("CPF")
        private String cpf;

        @JsonProperty("DataNascimento")
        private String dataNascimento;

        @JsonProperty("NomeFuncionario")
        private String nomeFuncionario;

        @JsonProperty("NomePai")
        private String nomePai;

        @JsonProperty("NomeMae")
        private String nomeMae;

        @JsonProperty("TipoDocumento")
        private String tipoDocumento;

        @JsonProperty("Sexo")
        private String sexo;

        @JsonProperty("Naturalidade")
        private String naturalidade;

        @JsonProperty("DataCadastro")
        private String dataCadastro;

        @JsonProperty("NumeroDocumento")
        private String numeroDocumento;

        @JsonProperty("UF")
        private String uf;

        @JsonProperty("EstadoCivil")
        private String estadoCivil;

        @JsonProperty("Nacionalidade")
        private String nacionalidade;

        @JsonProperty("ID")
        private String id;

        @JsonProperty("OrgaoEmissor")
        private String orgaoEmissor;

        @JsonProperty("DataEmissaoDocumento")
        private String dataEmissaoDocumento;

        @JsonProperty("CodNacionalidade")
        private String codNacionalidade;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Contato {

        @JsonProperty("Email")
        private String email;

        @JsonProperty("DDDTel")
        private String dddTel;

        @JsonProperty("Tel")
        private String tel;

        @JsonProperty("DDDCom")
        private String dddCom;

        @JsonProperty("TelCom")
        private String telCom;

        @JsonProperty("DDDCel")
        private String dddCel;

        @JsonProperty("Celular")
        private String celular;

        @JsonProperty("Endereco")
        private Endereco endereco;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Endereco {

        @JsonProperty("Endereco")
        private String endereco;

        @JsonProperty("CEP")
        private String cep;

        @JsonProperty("Cidade")
        private String cidade;

        @JsonProperty("UF")
        private String uf;

        @JsonProperty("Numero")
        private Integer numero;

        @JsonProperty("Complemento")
        private String complemento;

        @JsonProperty("Bairro")
        private String bairro;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DadosEmpresa {

        @JsonProperty("Cargo")
        private String cargo;

        @JsonProperty("TpVinculo")
        private String tpVinculo;

        @JsonProperty("Matricula")
        private String matricula;

        @JsonProperty("Situacao")
        private String situacao;

        @JsonProperty("DataAdmissao")
        private String dataAdmissao;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DadosBancarios {

        @JsonProperty("CodBanco")
        private String codBanco;

        @JsonProperty("TipodeConta")
        private String tipodeConta;

        @JsonProperty("NrConta")
        private String nrConta;

        @JsonProperty("ContaDig")
        private String contaDig;

        @JsonProperty("NrAgencia")
        private String nrAgencia;

        @JsonProperty("AgenciaDig")
        private String agenciaDig;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RendaMargem {

        @JsonProperty("TipoVinculo")
        private String tipoVinculo;

        @JsonProperty("RendaBruta")
        private Double rendaBruta;

        @JsonProperty("RendaLiquida")
        private Double rendaLiquida;

        @JsonProperty("MargemTotalParcelada")
        private Double margemTotalParcelada;

        @JsonProperty("MargemDispParcelar")
        private Double margemDispParcelar;

        @JsonProperty("MargemTotalCartao")
        private Double margemTotalCartao;

        @JsonProperty("MargemDispCartao")
        private Double margemDispCartao;

        @JsonProperty("RendaBrutaCript")
        private String rendaBrutaCript;

        @JsonProperty("RendaLiquidaCript")
        private String rendaLiquidaCript;

        @JsonProperty("MargemTotalParceladaCript")
        private String margemTotalParceladaCript;

        @JsonProperty("MargemDispParcelarCript")
        private String margemDispParcelarCript;

        @JsonProperty("MargemTotalCartaoCript")
        private String margemTotalCartaoCript;

        @JsonProperty("MargemDispCartaoCript")
        private String margemDispCartaoCript;

        @JsonProperty("IdFunc")
        private Integer idFunc;
    }


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Origem4 {

        @JsonProperty("CodOrg4")
        private String codOrg4;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Origem5 {

        @JsonProperty("CodOrg5")
        private String codOrg5;

        @JsonProperty("DescOrg5")
        private String descOrg5;
    }
}
