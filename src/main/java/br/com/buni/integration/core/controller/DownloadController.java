package br.com.buni.integration.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/download")
@Slf4j
public class DownloadController {

    @GetMapping("/{nomeArquivo}")
    public ResponseEntity<Resource> download(@PathVariable String nomeArquivo) {

        if (nomeArquivo.contains("..") || nomeArquivo.contains("/") || nomeArquivo.contains("\\")) {
            log.warn("Path traversal bloqueado: {}", nomeArquivo);
            return ResponseEntity.badRequest().build();
        }

        Path outputDir = Path.of("output").toAbsolutePath().normalize();
        Path caminho   = outputDir.resolve(nomeArquivo).normalize();

        if (!caminho.startsWith(outputDir)) {
            log.warn("Path traversal bloqueado (resolve): {}", nomeArquivo);
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(caminho);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

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
}
