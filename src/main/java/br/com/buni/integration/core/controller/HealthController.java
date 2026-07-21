package br.com.buni.integration.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${buni.ambiente:HML}")
    private String ambiente;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status",   "UP",
                "app",      "integration-engine",
                "ambiente", ambiente
        );
    }
}
