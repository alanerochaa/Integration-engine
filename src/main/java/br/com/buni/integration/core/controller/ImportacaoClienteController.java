package br.com.buni.integration.core.controller;

import br.com.buni.integration.core.model.csv.ClienteCsv;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.service.ClienteImportService;
import br.com.buni.integration.core.service.CsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/importar")
public class ImportacaoClienteController {

    private final CsvService csvService;
    private final ClienteImportService clienteImportService;

    @PostMapping("/clientes")
    public ResponseEntity<Map<String, Object>> importarClientes(
            @RequestParam("file") MultipartFile file
    ) {
        // getOriginalFilename() pode retornar null em uploads programáticos
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "arquivo.csv";

        List<ClienteCsv> clientes = csvService.lerClientes(file);

        ProcessamentoResult result = clienteImportService.processar(filename, clientes);

        String nomeRelatorio = result.getCaminhoRelatorio().getFileName().toString();
        String nomeExcel     = result.getCaminhoExcel().getFileName().toString();

        return ResponseEntity.ok(
                Map.of(
                        "status",        result.getStatusGeral(),
                        "arquivo",       filename,
                        "totalLinhas",   clientes.size(),
                        "totalSucesso",  result.getTotalSucesso(),
                        "totalErro",     result.getTotalErro(),
                        "totalDuplicado",result.getTotalDuplicado(),
                        "tempoTotalMs",  result.getTempoTotalMs(),
                        "relatorio",     nomeRelatorio,
                        "downloadUrl",   "/importar/relatorios/" + nomeRelatorio,
                        "excelUrl",      "/importar/relatorios/" + nomeExcel
                )
        );
    }

    @GetMapping("/relatorios/{nomeArquivo}")
    public ResponseEntity<Resource> baixarRelatorio(
            @PathVariable String nomeArquivo
    ) {
        // Prevenção de path traversal: rejeita nomes de arquivo com separadores de diretório ou ".."
        if (nomeArquivo.contains("..") || nomeArquivo.contains("/") || nomeArquivo.contains("\\")) {
            log.warn("Nome de arquivo suspeito bloqueado no download do relatório: {}", nomeArquivo);
            return ResponseEntity.badRequest().build();
        }

        Path outputDir = Path.of("output").toAbsolutePath().normalize();
        Path caminho   = outputDir.resolve(nomeArquivo).normalize();

        // Verifica que o caminho resolvido ainda está dentro do diretório de saída
        if (!caminho.startsWith(outputDir)) {
            log.warn("Tentativa de path traversal bloqueada: {}", nomeArquivo);
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
