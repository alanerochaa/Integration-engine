package br.com.buni.integration.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private DateUtils() {
    }

    private static final DateTimeFormatter FORMATO_SAIDA_FUNCIONARIO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatarDataFuncionario(String data) {

        if (data == null || data.isBlank()) {
            return "";
        }

        String input = data.trim();

        for (String pattern : new String[]{"dd/MM/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss"}) {
            try {
                return LocalDateTime.parse(input, DateTimeFormatter.ofPattern(pattern))
                        .format(FORMATO_SAIDA_FUNCIONARIO);
            } catch (Exception ignored) {}
        }

        String apenasData = input.split("[T ]")[0];
        for (String pattern : new String[]{"dd/MM/yyyy", "yyyy-MM-dd"}) {
            try {
                return LocalDate.parse(apenasData, DateTimeFormatter.ofPattern(pattern))
                        .atStartOfDay()
                        .format(FORMATO_SAIDA_FUNCIONARIO);
            } catch (Exception ignored) {}
        }

        return input;
    }

    public static String agoraIso() {
        return LocalDateTime.now().format(FORMATO_SAIDA_FUNCIONARIO);
    }

    public static String formatarDataBrParaIso(String data) {

        if (data == null || data.isBlank()) {
            return "";
        }

        try {

            String apenasData = data.trim().split(" ")[0];

            LocalDate localDate = LocalDate.parse(
                    apenasData,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

            return localDate.atStartOfDay().toString() + "Z";

        } catch (Exception e) {
            return data;
        }
    }

    public static String agora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}