package br.com.buni.integration.core.util;

import java.text.Normalizer;

public class StringUtils {

    private StringUtils() {
    }

    public static String limparNumero(
            String valor
    ) {

        if (
                valor == null
        ) {
            return "";
        }

        return valor.replaceAll(
                "\\D",
                ""
        );

    }

    public static String normalizarCpf(
            String cpf
    ) {

        String limpo =
                limparNumero(
                        cpf
                );

        if (
                limpo.length()
                        ==
                        10
        ) {

            return "0"
                    + limpo;

        }

        return limpo;

    }

    public static String removerAcentos(
            String texto
    ) {

        if (
                texto == null
        ) {

            return "";

        }

        return Normalizer
                .normalize(
                        texto,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                );

    }

    public static boolean vazio(
            String valor
    ) {

        return valor == null
                ||
                valor.isBlank();

    }

}