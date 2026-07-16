package br.com.buni.integration.core.validator;

import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import br.com.buni.integration.core.util.StringUtils;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClienteValidator {

    public List<String> validar(ClienteImportRow cliente) {

        List<String> erros = new ArrayList<>();

        validarObrigatorio(cliente.getCpf(), "CPF", erros);
        validarObrigatorio(cliente.getNome(), "NOME", erros);
        validarObrigatorio(cliente.getCep(), "CEP", erros);
        validarObrigatorio(cliente.getAgencia(), "CODAGENCIAPAGTO", erros);
        validarObrigatorio(cliente.getConta(), "CONTAPAGAMENTO", erros);
        validarBanco(cliente, erros);
        validarObrigatorio(cliente.getDataNascimento(), "DTNASCIMENTO", erros);

        // SALARIO não é mais obrigatório, pois o Mapper aplica fallback
        // validarObrigatorio(cliente.getSalario(), "SALARIO", erros);

        validarCpf(cliente.getCpf(), erros);
        validarEmail(cliente, erros);

        // Validação de renda removida: o Mapper aplica fallback para
        // salário vazio, inválido ou menor/igual a zero.
        // validarRenda(cliente.getSalario(), erros);

        return erros;
    }

    private void validarObrigatorio(String valor, String campo, List<String> erros) {
        if (StringUtils.vazio(valor)) {
            erros.add("Campo obrigatório ausente: " + campo);
        }
    }

    private void validarCpf(String cpf, List<String> erros) {

        if (StringUtils.vazio(cpf)) return;

        String limpo = StringUtils.normalizarCpf(cpf);

        if (limpo.length() != 11) {
            erros.add("CPF invalido: esperado 11 digitos, recebido " + limpo.length());
            return;
        }

        if (!isCpfValido(limpo)) {
            erros.add("CPF invalido: digitos verificadores incorretos (" + limpo + ")");
        }
    }

    /** Valida os dígitos verificadores do CPF pelo algoritmo padrão brasileiro. */
    private boolean isCpfValido(String cpf) {

        // CPFs com todos os dígitos iguais são matematicamente válidos, mas semanticamente inválidos
        if (cpf.chars().distinct().count() == 1) return false;

        int[] d = cpf.chars().map(c -> c - '0').toArray();

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += d[i] * (10 - i);
        int first = (sum * 10 % 11) % 10;
        if (first != d[9]) return false;

        sum = 0;
        for (int i = 0; i < 10; i++) sum += d[i] * (11 - i);
        int second = (sum * 10 % 11) % 10;

        return second == d[10];
    }

    private void validarEmail(ClienteImportRow cliente, List<String> erros) {

        String email = cliente.getEmailPessoal();

        if (StringUtils.vazio(email)) {
            email = cliente.getEmailProfissional();
        }

        if (StringUtils.vazio(email)) {
            erros.add("Email obrigatório ausente: EMAILPESSOAL ou EMAILPROFISSIONAL");
            return;
        }

        if (!email.contains("@")) {
            erros.add("Email inválido: " + email);
        }
    }

    /**
     * CODBANCOPAGTO tem prioridade sobre BANCO.
     * BANCO só é aceito como código bancário se for numérico.
     * Texto como "B.Uni" é ignorado — erro apenas se ambos forem inválidos/ausentes.
     */
    private void validarBanco(ClienteImportRow cliente, List<String> erros) {

        String codBancoPagto = StringUtils.limparNumero(cliente.getCompensacao());

        if (!codBancoPagto.isBlank()) return;

        String banco = StringUtils.limparNumero(cliente.getBanco());

        if (!banco.isBlank()) return;

        String valRecebido = "(CODBANCOPAGTO="
                + nvl(cliente.getCompensacao())
                + ", BANCO=" + nvl(cliente.getBanco()) + ")";

        erros.add("Banco inválido: preencher CODBANCOPAGTO com código numérico " + valRecebido);
    }

    private String nvl(String v) {
        return v != null ? v.trim() : "ausente";
    }
}
