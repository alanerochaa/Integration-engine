package br.com.buni.integration.core.validator;

import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import br.com.buni.integration.core.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FuncionarioValidator {

    private static final Set<String> UFS_VALIDAS = Set.of(
        "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
        "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
    );

    private static final Set<String> SITUACOES_VALIDAS = Set.of("AT", "AF", "DE");

    private static final Set<String> SEXOS_VALIDOS = Set.of("M", "F", "1", "2");

    public List<String> validar(FuncionarioImportRow row) {

        List<String> erros = new ArrayList<>();

        // ─── Obrigatórios ────────────────────────────────────────────────────────
        validarObrigatorio(row.getCpf(),                 "CPF",                  erros);
        validarObrigatorio(row.getNomeFuncionario(),      "NomeFuncionario",       erros);
        validarObrigatorio(row.getMatricula(),            "MATRICULA",            erros);
        validarObrigatorio(row.getDataAdmissao(),         "DATAADMISSAO",         erros);
        validarObrigatorio(row.getSituacao(),             "SITUACAO",             erros);
        validarObrigatorio(row.getRendaBruta(),           "RENDABRUTA",           erros);
        validarObrigatorio(row.getDataEmissaoDocumento(), "DATAEMISSAODOCUMENTO", erros);

        // ─── CPF ─────────────────────────────────────────────────────────────────
        validarCpf(row.getCpf(), erros);

        // ─── Renda ───────────────────────────────────────────────────────────────
        validarDouble(row.getRendaBruta(),   "RENDABRUTA",   true, erros);
        validarDouble(row.getRendaLiquida(), "RENDALIQUIDA", false, erros);

        // ─── Email ───────────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getEmail())) {
            validarEmail(row.getEmail(), erros);
        }

        // ─── CEP ─────────────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getCep())) {
            validarCep(row.getCep(), erros);
        }

        // ─── UF Endereço ─────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getUf())) {
            validarUf(row.getUf(), "UF", erros);
        }

        // ─── UF Documento ────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getUfDocumento())) {
            validarUf(row.getUfDocumento(), "UFDOCUMENTO", erros);
        }

        // ─── DDD ─────────────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getDddCel())) {
            validarDdd(row.getDddCel(), erros);
        }

        // ─── SITUACAO ────────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getSituacao())) {
            validarSituacao(row.getSituacao(), erros);
        }

        // ─── SEXO ────────────────────────────────────────────────────────────────
        if (!StringUtils.vazio(row.getSexo())) {
            validarSexo(row.getSexo(), erros);
        }

        return erros;
    }

    // ─── Regras ──────────────────────────────────────────────────────────────────

    private void validarObrigatorio(String valor, String campo, List<String> erros) {
        if (StringUtils.vazio(valor)) {
            erros.add("Campo obrigatório ausente: " + campo);
        }
    }

    private void validarCpf(String cpf, List<String> erros) {
        if (StringUtils.vazio(cpf)) return;
        String limpo = StringUtils.normalizarCpf(cpf);
        if (limpo.length() != 11) {
            erros.add("CPF inválido: esperado 11 dígitos, recebido " + limpo.length());
            return;
        }
        if (!isCpfValido(limpo)) {
            erros.add("CPF inválido: dígitos verificadores incorretos (" + limpo + ")");
        }
    }

    private void validarDouble(String valor, String campo, boolean obrigatorio, List<String> erros) {
        if (StringUtils.vazio(valor)) return;
        try {
            double v = Double.parseDouble(
                valor.trim()
                     .replace("R$", "")
                     .replace(" ", "")
                     .replace(".", "")
                     .replace(",", ".")
            );
            if (obrigatorio && v <= 0) {
                erros.add(campo + " inválido: deve ser um valor numérico positivo");
            }
        } catch (NumberFormatException e) {
            erros.add(campo + " inválido: valor não numérico (" + valor.trim() + ")");
        }
    }

    private void validarEmail(String email, List<String> erros) {
        String trimmed = email.trim();
        if (!trimmed.contains("@") || trimmed.indexOf('@') == 0 || trimmed.indexOf('@') == trimmed.length() - 1) {
            erros.add("EMAIL inválido: formato incorreto (" + trimmed + ")");
        }
    }

    private void validarCep(String cep, List<String> erros) {
        String limpo = StringUtils.limparNumero(cep);
        if (limpo.length() != 8) {
            erros.add("CEP inválido: deve conter 8 dígitos (" + cep.trim() + ")");
        }
    }

    private void validarUf(String uf, String campo, List<String> erros) {
        String normalizado = uf.trim().toUpperCase();
        if (!UFS_VALIDAS.contains(normalizado)) {
            erros.add(campo + " inválida: valor desconhecido (" + normalizado + ")");
        }
    }

    private void validarDdd(String ddd, List<String> erros) {
        String limpo = StringUtils.limparNumero(ddd);
        if (limpo.length() != 2) {
            erros.add("DDDCEL inválido: deve conter 2 dígitos (" + ddd.trim() + ")");
            return;
        }
        int codigo = Integer.parseInt(limpo);
        if (codigo < 11 || codigo > 99) {
            erros.add("DDDCEL inválido: código fora do intervalo válido (" + limpo + ")");
        }
    }

    private void validarSituacao(String situacao, List<String> erros) {
        String normalizado = situacao.trim().toUpperCase();
        if (!SITUACOES_VALIDAS.contains(normalizado)) {
            erros.add("SITUACAO inválida: valores aceitos são AT, AF, DE (" + normalizado + ")");
        }
    }

    private void validarSexo(String sexo, List<String> erros) {
        if (!SEXOS_VALIDOS.contains(sexo.trim().toUpperCase())) {
            erros.add("SEXO inválido: valores aceitos são M, F, 1, 2 (" + sexo.trim() + ")");
        }
    }

    private boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int[] d = cpf.chars().map(c -> c - '0').toArray();
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += d[i] * (10 - i);
        int primeiro = (soma * 10 % 11) % 10;
        if (primeiro != d[9]) return false;
        soma = 0;
        for (int i = 0; i < 10; i++) soma += d[i] * (11 - i);
        int segundo = (soma * 10 % 11) % 10;
        return segundo == d[10];
    }
}
