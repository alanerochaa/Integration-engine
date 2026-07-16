package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class FuncionarioExcelReader extends AbstractExcelReader<FuncionarioImportRow> {

    private static final Map<String, BiConsumer<FuncionarioImportRow, String>> COLUMN_SETTERS;

    static {
        Map<String, BiConsumer<FuncionarioImportRow, String>> m = new LinkedHashMap<>();

        // Dados Empresa
        m.put("MATRICULA",            (r, v) -> r.setMatricula(v));

        // Dados Pessoais
        m.put("CPF",                  (r, v) -> r.setCpf(v));
        m.put("NOMEFUNCIONARIO",      (r, v) -> r.setNomeFuncionario(v));
        m.put("NOMEPAI",              (r, v) -> r.setNomePai(v));
        m.put("NOMEMAE",              (r, v) -> r.setNomeMae(v));
        m.put("DATANASCIMENTO",       (r, v) -> r.setDataNascimento(v));
        m.put("SEXO",                 (r, v) -> r.setSexo(v));
        m.put("ESTADOCIVIL",          (r, v) -> r.setEstadoCivil(v));
        m.put("NATURALIDADE",         (r, v) -> r.setNaturalidade(v));
        m.put("NACIONALIDADE",        (r, v) -> r.setNacionalidade(v));
        m.put("CODNACIONALIDADE",     (r, v) -> r.setCodNacionalidade(v));
        m.put("TIPODOCUMENTO",        (r, v) -> r.setTipoDocumento(v));
        m.put("RG",                   (r, v) -> r.setRg(v));
        m.put("ORGAOEMISSOR",         (r, v) -> r.setOrgaoEmissor(v));
        m.put("UFDOCUMENTO",          (r, v) -> r.setUfDocumento(v));
        m.put("DATAEMISSAODOCUMENTO", (r, v) -> r.setDataEmissaoDocumento(v));
        m.put("DATACADASTRO",         (r, v) -> r.setDataCadastro(v));

        // Contato
        m.put("EMAIL",                (r, v) -> r.setEmail(v));
        m.put("DDDCEL",               (r, v) -> r.setDddCel(v));
        m.put("CELULAR",              (r, v) -> r.setCelular(v));

        // Endereço
        m.put("CEP",                  (r, v) -> r.setCep(v));
        m.put("LOGRADOURO",           (r, v) -> r.setLogradouro(v));
        m.put("NUMERO",               (r, v) -> r.setNumero(v));
        m.put("COMPLEMENTO",          (r, v) -> r.setComplemento(v));
        m.put("BAIRRO",               (r, v) -> r.setBairro(v));
        m.put("CIDADE",               (r, v) -> r.setCidade(v));
        m.put("UF",                   (r, v) -> r.setUf(v));

        // Dados Empresa (complemento)
        m.put("CARGO",                (r, v) -> r.setCargo(v));
        m.put("TPVINCULO",            (r, v) -> r.setTpVinculo(v));

        // Dados Bancários
        m.put("BANCO",                (r, v) -> r.setBanco(v));
        m.put("AGENCIA",              (r, v) -> r.setAgencia(v));
        m.put("CONTA",                (r, v) -> r.setConta(v));
        m.put("DIGITOCONTA",          (r, v) -> r.setDigitoConta(v));
        m.put("TIPODECONTA",          (r, v) -> r.setTipoDeConta(v));

        // Rendas
        m.put("RENDABRUTA",           (r, v) -> r.setRendaBruta(v));
        m.put("RENDALIQUIDA",         (r, v) -> r.setRendaLiquida(v));

        // Dados Empresa (conclusão)
        m.put("DATAADMISSAO",         (r, v) -> r.setDataAdmissao(v));
        m.put("SITUACAO",             (r, v) -> r.setSituacao(v));

        // Origens
        m.put("CODORG4",              (r, v) -> r.setCodOrg4(v));
        m.put("CODORG5",              (r, v) -> r.setCodOrg5(v));
        m.put("DESCORG5",             (r, v) -> r.setDescOrg5(v));

        COLUMN_SETTERS = Collections.unmodifiableMap(m);
    }

    @Override
    protected FuncionarioImportRow createRow() {
        return new FuncionarioImportRow();
    }

    @Override
    protected Map<String, BiConsumer<FuncionarioImportRow, String>> columnSetters() {
        return COLUMN_SETTERS;
    }
}
