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

    private final String nomeArquivo;

    private final String referencia;

    private final String contentType;

    private final long tamanhoBytes;
}
