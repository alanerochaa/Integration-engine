package br.com.buni.integration.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/download")
@Slf4j
public class DownloadController {

    @Value("${app.report.output-dir:output}")
    private String outputDir;

    @GetMapping("/{nomeArquivo}")
    public ResponseEntity<Resource> download(@PathVariable String nomeArquivo) {

        if (nomeArquivo.contains("..") || nomeArquivo.contains("/") || nomeArquivo.contains("\\")) {
            log.warn("[DOWNLOAD] Path traversal bloqueado: {}", nomeArquivo);
            return ResponseEntity.badRequest().build();
        }

        Path outputDirPath = Path.of(outputDir).toAbsolutePath().normalize();
        Path caminho       = outputDirPath.resolve(nomeArquivo).normalize();

        log.info("[DOWNLOAD] Solicitado — arquivo={} | diretório={} | caminho-resolvido={}",
                nomeArquivo, outputDirPath, caminho);

        if (!caminho.startsWith(outputDirPath)) {
            log.warn("[DOWNLOAD] Path traversal bloqueado (resolve): {}", nomeArquivo);
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(caminho);
        if (!resource.exists()) {
            log.warn("[DOWNLOAD] Arquivo não encontrado — caminho={} | diretório-existe={} | arquivos-no-diretório={}",
                    caminho,
                    Files.exists(outputDirPath),
                    listarArquivos(outputDirPath));
            return ResponseEntity.notFound().build();
        }

        try {
            long tamanho = Files.size(caminho);
            log.info("[DOWNLOAD] Arquivo localizado — caminho={} | tamanho={} bytes", caminho, tamanho);
        } catch (Exception ignored) {}

        boolean isExcel = nomeArquivo.endsWith(".xlsx");
        MediaType contentType = isExcel
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.TEXT_HTML;
        ContentDisposition disposition = isExcel
                ? ContentDisposition.attachment().filename(nomeArquivo).build()
                : ContentDisposition.inline().filename(nomeArquivo).build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private String listarArquivos(Path dir) {
        try {
            if (!Files.exists(dir)) return "(diretório inexistente)";
            try (var stream = Files.list(dir)) {
                return stream.map(p -> p.getFileName().toString())
                             .reduce((a, b) -> a + ", " + b)
                             .orElse("(vazio)");
            }
        } catch (Exception e) {
            return "(erro ao listar: " + e.getMessage() + ")";
        }
    }
}
