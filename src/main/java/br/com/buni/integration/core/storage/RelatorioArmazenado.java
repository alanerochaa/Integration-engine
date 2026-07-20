package br.com.buni.integration.core.storage;

import lombok.Builder;
import lombok.Getter;

/**
 * Resultado imutável de uma operação de armazenamento de relatório.
 *
 * "referencia" é a chave opaca que identifica o arquivo no backend de
 * armazenamento escolhido:
 *  - LocalRelatorioStorage  → nome do arquivo   (ex: relatorio_xxx.xlsx)
 *  - AzureBlobRelatorioStorage → nome do blob   (ex: imports/2026/relatorio_xxx.xlsx)
 *  - S3RelatorioStorage     → object key        (ex: reports/relatorio_xxx.xlsx)
 *
 * O DownloadController usa esta referência para recuperar o conteúdo via
 * RelatorioStorage.recuperar(referencia), independentemente de onde o arquivo
 * esteja armazenado.
 */
@Getter
@Builder
public class RelatorioArmazenado {

    /** Nome do arquivo no formato sanitizado (ex: relatorio_1234_clientes.xlsx). */
    private final String nomeArquivo;

    /**
     * Chave de recuperação opaca — o significado varia por implementação.
     * Para download, o frontend recebe /download/{referencia}.
     */
    private final String referencia;

    /** MIME type do conteúdo (ex: application/vnd.openxmlformats-..., text/html). */
    private final String contentType;

    /** Tamanho em bytes do arquivo armazenado. */
    private final long tamanhoBytes;
}
