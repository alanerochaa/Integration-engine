package br.com.buni.integration.core.templates.service;

import br.com.buni.integration.core.templates.dto.TemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TemplateService {

    static final Path TEMPLATES_DIR = Path.of("data/templates");

    private static final String PREFIXO = "Modelo_Importacao_";
    private static final Set<String> FORMATOS_SUPORTADOS = Set.of("xlsx", "csv");

    // ─── Listagem dinâmica ────────────────────────────────────────────────────────

    public List<TemplateDto> listar() {

        Map<String, List<String>> agrupado = new TreeMap<>();

        try {
            if (!Files.exists(TEMPLATES_DIR)) return List.of();

            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(TEMPLATES_DIR, PREFIXO + "*")) {

                for (Path arquivo : stream) {
                    parsear(arquivo.getFileName().toString()).ifPresent(e ->
                        agrupado.computeIfAbsent(e.tipoNome(), k -> new ArrayList<>())
                                .add(e.formato())
                    );
                }
            }
        } catch (IOException ex) {
            log.error("Erro ao listar templates: {}", ex.getMessage());
        }

        return agrupado.entrySet().stream()
                .map(e -> {
                    String tipoNome = e.getKey();
                    List<String> formatos = e.getValue().stream().sorted().collect(Collectors.toList());
                    return TemplateDto.builder()
                            .id(tipoNome.toLowerCase())
                            .nome("Modelo de Importação - " + nomeExibicao(tipoNome))
                            .tipo(tipoNome.toUpperCase())
                            .formatos(formatos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Download ─────────────────────────────────────────────────────────────────

    public Resource obter(String id, String formato) {

        String fmt = formato.toLowerCase();
        if (!FORMATOS_SUPORTADOS.contains(fmt)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Formato não suportado: " + formato + ". Formatos aceitos: " + FORMATOS_SUPORTADOS);
        }

        return buscarArquivo(id, fmt)
                .map(FileSystemResource::new)
                .map(r -> (Resource) r)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Modelo de importação não encontrado: " + id + "/" + formato));
    }

    // ─── Internos ─────────────────────────────────────────────────────────────────

    private Optional<Path> buscarArquivo(String id, String formato) {

        if (!Files.exists(TEMPLATES_DIR)) return Optional.empty();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(TEMPLATES_DIR, PREFIXO + "*." + formato)) {

            for (Path arquivo : stream) {
                Optional<ArquivoInfo> info = parsear(arquivo.getFileName().toString());
                if (info.isPresent() && info.get().tipoNome().equalsIgnoreCase(id)) {
                    return Optional.of(arquivo);
                }
            }
        } catch (IOException ex) {
            log.error("Erro ao buscar template {}/{}: {}", id, formato, ex.getMessage());
        }

        return Optional.empty();
    }

    private Optional<ArquivoInfo> parsear(String filename) {

        if (!filename.startsWith(PREFIXO)) return Optional.empty();

        String resto  = filename.substring(PREFIXO.length());
        int    dotIdx = resto.lastIndexOf('.');
        if (dotIdx < 1) return Optional.empty();

        String tipoNome = resto.substring(0, dotIdx);
        String formato  = resto.substring(dotIdx + 1).toLowerCase();

        if (tipoNome.isBlank() || !FORMATOS_SUPORTADOS.contains(formato)) return Optional.empty();

        return Optional.of(new ArquivoInfo(tipoNome, formato));
    }

    private String nomeExibicao(String tipoNome) {
        return switch (tipoNome.toUpperCase()) {
            case "FUNCIONARIOS" -> "Funcionários";
            case "CLIENTES"     -> "Clientes";
            default             -> tipoNome;
        };
    }

    // ─── Record interno ───────────────────────────────────────────────────────────

    private record ArquivoInfo(String tipoNome, String formato) {}
}
