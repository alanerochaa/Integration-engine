package br.com.buni.integration.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileUtils {

    private FileUtils() {
    }

    public static void criarDiretorio(String caminho) {

        try {

            Files.createDirectories(
                    Path.of(caminho)
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erro ao criar diretório: "
                            + caminho,
                    e
            );

        }
    }

    public static String gerarNomeRelatorio(
            String nomeArquivo
    ) {

        String dataHora =
                LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd_HHmmss"
                                )
                        );

        return "relatorio_"
                + dataHora
                + "_"
                + nomeArquivo;

    }

    public static String obterExtensao(
            String arquivo
    ) {

        if (
                arquivo == null
                        ||
                        !arquivo.contains(".")
        ) {
            return "";
        }

        return arquivo.substring(
                arquivo.lastIndexOf(".")
        );

    }

}