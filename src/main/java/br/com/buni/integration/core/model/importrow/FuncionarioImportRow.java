package br.com.buni.integration.core.model.importrow;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import lombok.Data;

@Data
public class FuncionarioImportRow {

    // ─── Dados da Empresa ────────────────────────────────────────────────────────
    @CsvBindByName(column = "MATRICULA")
    private String matricula;

    // ─── Dados Pessoais ──────────────────────────────────────────────────────────
    @CsvBindByName(column = "CPF")
    private String cpf;

    @CsvBindByName(column = "NomeFuncionario")
    private String nomeFuncionario;

    @CsvBindByName(column = "NomePai")
    private String nomePai;

    @CsvBindByName(column = "NomeMae")
    private String nomeMae;

    @CsvBindByName(column = "DATANASCIMENTO")
    private String dataNascimento;

    @CsvBindByName(column = "SEXO")
    private String sexo;

    @CsvBindByName(column = "ESTADOCIVIL")
    private String estadoCivil;

    @CsvBindByName(column = "NATURALIDADE")
    private String naturalidade;

    @CsvBindByName(column = "NACIONALIDADE")
    private String nacionalidade;

    @CsvBindByName(column = "CODNACIONALIDADE")
    private String codNacionalidade;

    @CsvBindByName(column = "TIPODOCUMENTO")
    private String tipoDocumento;

    @CsvBindByName(column = "RG")
    private String rg;

    @CsvBindByName(column = "ORGAOEMISSOR")
    private String orgaoEmissor;

    @CsvBindByName(column = "UFDOCUMENTO")
    private String ufDocumento;

    @CsvBindByName(column = "DATAEMISSAODOCUMENTO")
    private String dataEmissaoDocumento;

    @CsvBindByName(column = "DATACADASTRO")
    private String dataCadastro;

    // ─── Contato ─────────────────────────────────────────────────────────────────
    @CsvBindByName(column = "EMAIL")
    private String email;

    @CsvBindByName(column = "DDDCEL")
    private String dddCel;

    @CsvBindByName(column = "CELULAR")
    private String celular;

    // ─── Endereço ────────────────────────────────────────────────────────────────
    @CsvBindByName(column = "CEP")
    private String cep;

    @CsvBindByName(column = "LOGRADOURO")
    private String logradouro;

    @CsvBindByName(column = "NUMERO")
    private String numero;

    @CsvBindByName(column = "COMPLEMENTO")
    private String complemento;

    @CsvBindByName(column = "BAIRRO")
    private String bairro;

    @CsvBindByName(column = "CIDADE")
    private String cidade;

    @CsvBindByName(column = "UF")
    private String uf;

    // ─── Dados Empresa (complemento) ─────────────────────────────────────────────
    @CsvBindByName(column = "CARGO")
    private String cargo;

    @CsvBindByName(column = "TPVINCULO")
    private String tpVinculo;

    // ─── Dados Bancários ─────────────────────────────────────────────────────────
    @CsvBindByName(column = "BANCO")
    private String banco;

    @CsvBindByName(column = "AGENCIA")
    private String agencia;

    @CsvBindByName(column = "CONTA")
    private String conta;

    @CsvBindByName(column = "DIGITOCONTA")
    private String digitoConta;

    @CsvBindByName(column = "TIPODECONTA")
    private String tipoDeConta;

    // ─── Rendas ──────────────────────────────────────────────────────────────────
    @CsvBindByName(column = "RENDABRUTA")
    private String rendaBruta;

    @CsvBindByName(column = "RENDALIQUIDA")
    private String rendaLiquida;

    // ─── Dados Empresa (conclusão) ────────────────────────────────────────────────
    @CsvBindByName(column = "DATAADMISSAO")
    private String dataAdmissao;

    @CsvBindByName(column = "SITUACAO")
    private String situacao;

    // ─── Origens ─────────────────────────────────────────────────────────────────
    @CsvBindByName(column = "CodOrg4")
    private String codOrg4;

    @CsvBindByName(column = "CodOrg5")
    private String codOrg5;

    @CsvBindByName(column = "DescOrg5")
    private String descOrg5;

    // ─── Controle interno ────────────────────────────────────────────────────────
    @CsvIgnore
    private String observacao;
}
