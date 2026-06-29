package br.com.buni.integration.core.report;

import br.com.buni.integration.core.model.dto.ExecutionReportLine;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ImportacaoReportGenerator {

    private static final String OUTPUT_DIR = "output";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Path gerarRelatorioCsv(
            String nomeArquivo,
            List<ExecutionReportLine> linhas
    ) {

        try {

            Files.createDirectories(
                    Path.of(OUTPUT_DIR)
            );

            String nomeBase =
                    limparNomeArquivo(
                            nomeArquivo
                    );

            String reportName =
                    "relatorio_"
                            + System.currentTimeMillis()
                            + "_"
                            + nomeBase;

            Path caminho =
                    Path.of(
                            OUTPUT_DIR,
                            reportName
                    );

            try (
                    BufferedWriter writer =
                            new BufferedWriter(
                                    new FileWriter(
                                            caminho.toFile()
                                    )
                            )
            ) {

                writer.write(
                        "DataHora;Arquivo;LinhaCSV;CPF;Nome;Status;CampoErro;ValorRecebido;MotivoErro;AcaoSugerida;CodigoCliente;MensagemApi;FallbackAplicado;Observacao"
                );

                writer.newLine();

                for (
                        ExecutionReportLine linha :
                        linhas
                ) {

                    writer.write(
                            formatarLinha(
                                    linha
                            )
                    );

                    writer.newLine();

                }

            }

            return caminho;

        } catch (
                Exception e
        ) {

            throw new RuntimeException(
                    "Erro ao gerar relatório de execução: "
                            + e.getMessage(),
                    e
            );

        }

    }

    private String limparNomeArquivo(
            String nomeArquivo
    ) {

        if (
                nomeArquivo == null
                        ||
                        nomeArquivo.isBlank()
        ) {

            return "arquivo.csv";

        }

        String nome =
                nomeArquivo
                        .replace("\\", "_")
                        .replace("/", "_")
                        .replace(" ", "_");

        if (
                !nome.endsWith(".csv")
        ) {

            nome =
                    nome
                            + ".csv";

        }

        return nome;

    }

    private String formatarLinha(
            ExecutionReportLine linha
    ) {

        return String.join(
                ";",
                valor(
                        linha.getDataHoraExecucao() != null
                                ? linha.getDataHoraExecucao().format(
                                FORMATTER
                        )
                                : ""
                ),
                valor(
                        linha.getNomeArquivo()
                ),
                valor(
                        linha.getLinhaCsv()
                ),
                valor(
                        linha.getCpf()
                ),
                valor(
                        linha.getNome()
                ),
                valor(
                        linha.getStatus()
                ),
                valor(
                        linha.getCampoErro()
                ),
                valor(
                        linha.getValorRecebido()
                ),
                valor(
                        linha.getMotivoErro()
                ),
                valor(
                        linha.getAcaoSugerida()
                ),
                valor(
                        linha.getProtocoloApi()
                ),
                valor(
                        linha.getMensagemApi()
                ),
                valor(
                        linha.getFallbackAplicado()
                ),
                valor(
                        linha.getObservacao()
                )
        );

    }

    private String valor(
            Object valor
    ) {

        if (
                valor == null
        ) {

            return "";

        }

        return valor
                .toString()
                .replace(
                        ";",
                        ","
                )
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                );

    }

}