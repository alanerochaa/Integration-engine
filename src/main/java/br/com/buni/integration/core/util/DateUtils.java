package br.com.buni.integration.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private DateUtils() {
    }

    public static String formatarDataBrParaIso(
            String data
    ) {

        if (
                data == null
                        ||
                        data.isBlank()
        ) {
            return "";
        }

        try {

            String apenasData =
                    data
                            .trim()
                            .split(" ")[0];

            LocalDate localDate =
                    LocalDate.parse(
                            apenasData,
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy"
                            )
                    );

            return localDate
                    .atStartOfDay()
                    .toString()
                    + "Z";

        } catch (Exception e) {

            return data;

        }

    }

    public static String agora() {

        return LocalDateTime
                .now()
                .format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm:ss"
                        )
                );

    }

}