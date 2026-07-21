package br.com.buni.integration.core.util;

import java.text.Normalizer;

public class StringUtils {

    private StringUtils() {}

    public static String limparNumero(String valor) {
        if (valor == null) return "";
        return valor.replaceAll("\\D", "");
    }

    public static String normalizarCpf(String cpf) {
        String limpo = limparNumero(cpf);
        return limpo.length() == 10 ? "0" + limpo : limpo;
    }

    public static String removerAcentos(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "");
    }

    public static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public static String sanitizarNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) return "arquivo";
        nome = nome.trim();
        while (nome.toLowerCase().endsWith(".csv")) nome = nome.substring(0, nome.length() - 4);
        nome = removerAcentos(nome);
        nome = nome.replace("\\", "_").replace("/", "_").replace(" ", "_");
        nome = nome.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        nome = nome.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return nome.isBlank() ? "arquivo" : nome;
    }
}
