package br.com.buni.integration.core.storage.local;

import br.com.buni.integration.core.storage.RelatorioArmazenado;
import br.com.buni.integration.core.storage.RelatorioStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementação de RelatorioStorage baseada no sistema de arquivos local.
 *
 * Esta classe extrai e centraliza a lógica de I/O que hoje está distribuída
 * entre ExcelReportGenerator, ImportacaoReportGenerator e DownloadController.
 *
 * Status: DEFINIDA — ainda não conectada aos geradores.
 * Os geradores continuam usando FileOutputStream/BufferedWriter diretamente
 * até que a Sprint de migração seja aprovada e executada.
 *
 * Para substituir por Azure Blob ou S3:
 *   1. Criar AzureBlobRelatorioStorage (ou S3RelatorioStorage) implements RelatorioStorage
 *   2. Anotar a nova implementação com @Primary
 *   3. Nenhuma outra classe de negócio precisa ser alterada
 */
@Component
@Slf4j
public class LocalRelatorioStorage implements RelatorioStorage {

    @Value("${app.report.output-dir:output}")
    private String outputDir;

    @Override
    public RelatorioArmazenado salvar(String nomeArquivo, byte[] conteudo, String contentType) {
        try {
            Path dir     = Path.of(outputDir).toAbsolutePath().normalize();
            Path caminho = dir.resolve(nomeArquivo);
            Files.createDirectories(dir);
            Files.write(caminho, conteudo);
            long tamanho = Files.size(caminho);
            log.info("[STORAGE-LOCAL] Salvo — path={} | tamanho={} bytes", caminho, tamanho);
            return RelatorioArmazenado.builder()
                    .nomeArquivo(nomeArquivo)
                    .referencia(nomeArquivo)
                    .contentType(contentType)
                    .tamanhoBytes(tamanho)
                    .build();
        } catch (Exception e) {
            log.error("[STORAGE-LOCAL] Falha ao salvar {} — outputDir={} | erro={}",
                    nomeArquivo, outputDir, e.getMessage(), e);
            throw new RuntimeException("Erro ao salvar relatório: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource recuperar(String referencia) {
        Path caminho = Path.of(outputDir).toAbsolutePath().normalize().resolve(referencia);
        log.info("[STORAGE-LOCAL] Recuperando — referencia={} | caminho={}", referencia, caminho);
        return new FileSystemResource(caminho);
    }

    @Override
    public boolean existe(String referencia) {
        Path caminho = Path.of(outputDir).toAbsolutePath().normalize().resolve(referencia);
        boolean existe = Files.exists(caminho);
        log.debug("[STORAGE-LOCAL] existe={} | caminho={}", existe, caminho);
        return existe;
    }
}
