package br.com.buni.integration.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/configuracoes")
public class ConfiguracaoController {

    @Value("${buni.ambiente:HML}")
    private String ambiente;

    @Value("${buni.csv.charset:UTF-8}")
    private String csvCharset;

    @Value("${buni.funcionario.base-url:}")
    private String apiFuncionarioUrl;

    @Value("${buni.cliente.base-url:}")
    private String apiClienteUrl;

    @GetMapping
    public ResponseEntity<Map<String, Object>> obter() {

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("ambiente",          ambiente);
        config.put("csvCharset",        csvCharset);
        config.put("maxFileSizeMb",     50);
        config.put("timeoutConexaoMs",  10_000);
        config.put("timeoutLeituraMs",  30_000);
        config.put("apiClienteUrl",     apiClienteUrl);
        config.put("apiFuncionarioUrl", apiFuncionarioUrl);

        return ResponseEntity.ok(config);
    }
}
