package br.com.buni.integration.core.model.request;

import lombok.Data;

import java.util.List;

@Data
public class ClienteRequest {

    private String tipoPessoa;
    private String cpf;
    private String nome;
    private String nacionalidade;
    private String tipoDocumento;
    private String numeroDocumento;
    private String ufDocumento;
    private String dataEmissaoDocumento;
    private String orgaoEmissorDocumento;
    private String estadoCivil;
    private String sexo;
    private String dataNascimento;
    private Double valorRenda;
    private Boolean pep;
    private String escolaridade;
    private String naturalidade;
    private String email;

    private DadoEndereco dadoEndereco;
    private Telefones telefones;
    private DadoProfissional dadoProfissional;
    private DadosBancarios dadosBancarios;

    @Data
    public static class DadoEndereco {
        private String tipoResidencia;
        private String residenciaAnteriorAnos;
        private String residenciaAnteriorMeses;
        private Enderecos enderecos;
    }

    @Data
    public static class Enderecos {
        private List<Endereco> endereco;
    }

    @Data
    public static class Endereco {
        private String tipo;
        private String cep;
        private String logradouro;
        private String numero;
        private String bairro;
        private String cidade;
        private String uf;
    }

    @Data
    public static class Telefones {
        private List<Telefone> telefone;
    }

    @Data
    public static class Telefone {
        private String tipo;
        private String ddd;
        private String numeroTelefone;
    }

    @Data
    public static class DadoProfissional {
        private String empresaNome;
        private String empresaCnpj;
        private String profissao;
        private String cargo;
        private String dataAdmissao;
        private String numeroMatricula;
    }

    @Data
    public static class DadosBancarios {
        private List<DadoBancario> dadoBancario;
    }

    @Data
    public static class DadoBancario {
        private String compensacao;
        private String banco;
        private String agencia;
        private String agenciaDigito;
        private String conta;
        private String contaDigito;
        private String tipoConta;
    }
}