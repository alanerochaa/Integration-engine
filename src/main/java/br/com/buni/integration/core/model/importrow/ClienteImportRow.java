package br.com.buni.integration.core.model.importrow;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import lombok.Data;

@Data
public class ClienteImportRow {

    @CsvBindByName(column = "CNPJ")
    private String empresaCnpj;

    @CsvBindByName(column = "NOMEFANTASIA")
    private String empresaNome;

    @CsvBindByName(column = "CHAPA")
    private String numeroMatricula;

    @CsvBindByName(column = "NOME")
    private String nome;

    @CsvBindByName(column = "DATAADMISSAO")
    private String dataAdmissao;

    @CsvBindByName(column = "FUNCAO")
    private String cargo;

    @CsvBindByName(column = "GRAUINSTRUCAO")
    private String escolaridade;

    @CsvBindByName(column = "DTNASCIMENTO")
    private String dataNascimento;

    @CsvBindByName(column = "SITUACAO")
    private String situacao;

    @CsvBindByName(column = "CPF")
    private String cpf;

    @CsvBindByName(column = "SALARIO")
    private String salario;

    @CsvBindByName(column = "CEP")
    private String cep;

    @CsvBindByName(column = "RUA")
    private String logradouro;

    @CsvBindByName(column = "HORARIO")
    private String horario;

    @CsvBindByName(column = "NUMERO")
    private String numero;

    @CsvBindByName(column = "BANCO")
    private String banco;

    @CsvBindByName(column = "CODBANCOPAGTO")
    private String compensacao;

    @CsvBindByName(column = "CODAGENCIAPAGTO")
    private String agencia;

    @CsvBindByName(column = "CONTAPAGAMENTO")
    private String conta;

    @CsvBindByName(column = "TELEFONE1")
    private String telefone1;

    @CsvBindByName(column = "TELEFONE2")
    private String telefone2;

    @CsvBindByName(column = "EMAILPROFISSIONAL")
    private String emailProfissional;

    @CsvBindByName(column = "EMAILPESSOAL")
    private String emailPessoal;

    @CsvBindByName(column = "SEXO")
    private String sexo;

    @CsvBindByName(column = "CODSECAO")
    private String codSecao;

    @CsvIgnore
    private Boolean enderecoFallbackAplicado;

    @CsvIgnore
    private String observacaoEndereco;
}
