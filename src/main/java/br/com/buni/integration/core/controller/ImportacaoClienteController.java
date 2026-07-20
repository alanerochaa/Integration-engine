package br.com.buni.integration.core.controller;

import br.com.buni.integration.core.domain.importacao.TipoImportacao;
import br.com.buni.integration.core.model.dto.ImportacaoResponse;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import br.com.buni.integration.core.service.ClienteImportService;
import br.com.buni.integration.core.service.FileImportService;
import br.com.buni.integration.core.service.FuncionarioImportService;
import br.com.buni.integration.core.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/importar")
public class ImportacaoClienteController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final FileImportService fileImportService;
    private final ClienteImportService clienteImportService;
    private final FuncionarioImportService funcionarioImportService;
    private final HistoricoService historicoService;

    @PostMapping("/clientes")
    public ResponseEntity<ImportacaoResponse> importarClientes(
            @RequestParam("file") MultipartFile file
    ) {
        String filename = resolverNomeArquivo(file);
        List<ClienteImportRow> clientes = fileImportService.lerClientes(file);
        ProcessamentoResult result = clienteImportService.processar(filename, clientes);
        historicoService.registrar(TipoImportacao.CLIENTES, filename, clientes.size(), result);
        return ResponseEntity.ok(toResponse(clientes.size(), result));
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<ImportacaoResponse> importarFuncionarios(
            @RequestParam("file") MultipartFile file
    ) {
        String filename = resolverNomeArquivo(file);
        List<FuncionarioImportRow> funcionarios = fileImportService.lerFuncionarios(file);
        ProcessamentoResult result = funcionarioImportService.processar(filename, funcionarios);
        historicoService.registrar(TipoImportacao.FUNCIONARIOS, filename, funcionarios.size(), result);
        return ResponseEntity.ok(toResponse(funcionarios.size(), result));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private ImportacaoResponse toResponse(long processados, ProcessamentoResult result) {
        String nomeRelatorio = result.getCaminhoRelatorio().getFileName().toString();
        String nomeExcel     = result.getCaminhoExcel().getFileName().toString();
        String base          = baseUrl.stripTrailing().replaceAll("/$", "");
        return ImportacaoResponse.builder()
                .importId(result.getImportId())
                .status(result.getStatusGeral())
                .processados(processados)
                .sucesso(result.getTotalSucesso())
                .erro(result.getTotalErro())
                .duplicado(result.getTotalDuplicado())
                .tempoMs(result.getTempoTotalMs())
                .downloadUrl(base + "/download/" + nomeRelatorio)
                .excelUrl(base + "/download/" + nomeExcel)
                .build();
    }

    private String resolverNomeArquivo(MultipartFile file) {
        String nome = file.getOriginalFilename();
        return (nome != null && !nome.isBlank()) ? nome : "arquivo.csv";
    }
}
