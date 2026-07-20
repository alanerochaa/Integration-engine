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

    /**
     * Sanitiza o nome de um arquivo de origem para uso seguro em nomes de relatório.
     *
     * Ordem das transformações:
     *  1. Trim de espaços externos
     *  2. Remove extensões .csv (tratamento de dupla extensão do Windows)
     *  3. Normaliza acentos: ã→a, é→e, ç→c, etc.
     *  4. Substitui barras e espaços por underscore
     *  5. Remove qualquer caractere fora de [a-zA-Z0-9_-]
     *  6. Colapsa múltiplos underscores consecutivos em um único
     *  7. Remove underscores nas extremidades
     *  8. Fallback para "arquivo" se o resultado estiver vazio
     */
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
