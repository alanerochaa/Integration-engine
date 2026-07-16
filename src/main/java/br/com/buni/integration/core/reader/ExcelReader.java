package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class ExcelReader extends AbstractExcelReader<ClienteImportRow> {

    private static final Map<String, BiConsumer<ClienteImportRow, String>> COLUMN_SETTERS;

    static {
        Map<String, BiConsumer<ClienteImportRow, String>> m = new LinkedHashMap<>();
        m.put("CNPJ",              (c, v) -> c.setEmpresaCnpj(v));
        m.put("NOMEFANTASIA",      (c, v) -> c.setEmpresaNome(v));
        m.put("CHAPA",             (c, v) -> c.setNumeroMatricula(v));
        m.put("NOME",              (c, v) -> c.setNome(v));
        m.put("DATAADMISSAO",      (c, v) -> c.setDataAdmissao(v));
        m.put("FUNCAO",            (c, v) -> c.setCargo(v));
        m.put("GRAUINSTRUCAO",     (c, v) -> c.setEscolaridade(v));
        m.put("DTNASCIMENTO",      (c, v) -> c.setDataNascimento(v));
        m.put("SITUACAO",          (c, v) -> c.setSituacao(v));
        m.put("CPF",               (c, v) -> c.setCpf(v));
        m.put("SALARIO",           (c, v) -> c.setSalario(v));
        m.put("CEP",               (c, v) -> c.setCep(v));
        m.put("RUA",               (c, v) -> c.setLogradouro(v));
        m.put("HORARIO",           (c, v) -> c.setHorario(v));
        m.put("NUMERO",            (c, v) -> c.setNumero(v));
        m.put("BANCO",             (c, v) -> c.setBanco(v));
        m.put("CODBANCOPAGTO",     (c, v) -> c.setCompensacao(v));
        m.put("CODAGENCIAPAGTO",   (c, v) -> c.setAgencia(v));
        m.put("CONTAPAGAMENTO",    (c, v) -> c.setConta(v));
        m.put("TELEFONE1",         (c, v) -> c.setTelefone1(v));
        m.put("TELEFONE2",         (c, v) -> c.setTelefone2(v));
        m.put("EMAILPROFISSIONAL", (c, v) -> c.setEmailProfissional(v));
        m.put("EMAILPESSOAL",      (c, v) -> c.setEmailPessoal(v));
        m.put("SEXO",              (c, v) -> c.setSexo(v));
        m.put("CODSECAO",          (c, v) -> c.setCodSecao(v));
        COLUMN_SETTERS = Collections.unmodifiableMap(m);
    }

    @Override
    protected ClienteImportRow createRow() {
        return new ClienteImportRow();
    }

    @Override
    protected Map<String, BiConsumer<ClienteImportRow, String>> columnSetters() {
        return COLUMN_SETTERS;
    }
}
