package br.com.buni.integration.core.storage;

import org.springframework.core.io.Resource;

/**
 * Porta de saída para armazenamento e recuperação de relatórios.
 *
 * Implementações previstas:
 *  - LocalRelatorioStorage          → sistema de arquivos local (atual)
 *  - AzureBlobRelatorioStorage      → Azure Blob Storage
 *  - S3RelatorioStorage             → AWS S3
 *
 * Para trocar o backend de armazenamento, basta registrar uma nova
 * implementação como @Primary — nenhum código de negócio precisa mudar.
 */
public interface RelatorioStorage {

    /**
     * Persiste o conteúdo de um relatório e retorna os metadados do armazenamento.
     *
     * @param nomeArquivo nome do arquivo já sanitizado (sem caminhos)
     * @param conteudo    bytes do arquivo a ser armazenado
     * @param contentType MIME type do conteúdo
     * @return metadados do arquivo armazenado, incluindo a referência de recuperação
     */
    RelatorioArmazenado salvar(String nomeArquivo, byte[] conteudo, String contentType);

    /**
     * Recupera um relatório como Resource streamável.
     * Usado pelo DownloadController para servir o arquivo ao cliente.
     *
     * @param referencia chave opaca retornada por {@link #salvar}
     * @return resource pronto para ser enviado na resposta HTTP
     */
    Resource recuperar(String referencia);

    /**
     * Verifica se um relatório existe no armazenamento.
     * Usado pelo DownloadController antes de tentar a recuperação.
     *
     * @param referencia chave opaca retornada por {@link #salvar}
     */
    boolean existe(String referencia);
}
