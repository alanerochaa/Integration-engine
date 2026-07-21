package br.com.buni.integration.core.controller;

import br.com.buni.integration.core.model.dto.HistoricoImportacao;
import br.com.buni.integration.core.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/importacoes")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    public ResponseEntity<List<HistoricoImportacao>> listar() {
        return ResponseEntity.ok(historicoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoricoImportacao> buscarPorId(@PathVariable String id) {
        return historicoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Importação não encontrada: " + id));
    }
}
