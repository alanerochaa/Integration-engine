package br.com.buni.integration.core.templates.controller;

import br.com.buni.integration.core.templates.dto.TemplateDto;
import br.com.buni.integration.core.templates.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TemplateDto>> listar() {
        return ResponseEntity.ok(templateService.listar());
    }

    @GetMapping("/{id}/{formato}")
    public ResponseEntity<Resource> download(
            @PathVariable String id,
            @PathVariable String formato) {

        Resource resource = templateService.obter(id, formato);

        String fmt  = formato.toLowerCase();
        String nome = "Modelo_Importacao_" + id + "." + fmt;

        MediaType contentType = switch (fmt) {
            case "xlsx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv"  -> MediaType.parseMediaType("text/csv;charset=UTF-8");
            default     -> MediaType.APPLICATION_OCTET_STREAM;
        };

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(nome)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDisposition(disposition);

        try {
            headers.setContentLength(resource.contentLength());
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
