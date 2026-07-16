package br.com.buni.integration.core.service;

import br.com.buni.integration.core.model.dto.HistoricoImportacao;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.util.DateUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoService {

    private static final Path ARQUIVO = Path.of("data/historico.json");

    private final ObjectMapper objectMapper;
    private final List<HistoricoImportacao> historico = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void carregar() {
        try {
            if (!Files.exists(ARQUIVO)) {
                log.info("Historico: nenhum arquivo anterior encontrado. Iniciando vazio.");
                return;
            }
            List<HistoricoImportacao> carregados = objectMapper.readValue(
                ARQUIVO.toFile(),
                new TypeReference<List<HistoricoImportacao>>() {}
            );
            historico.addAll(carregados);
            log.info("Historico carregado: {} importacoes anteriores.", historico.size());
        } catch (Exception e) {
            log.error("Erro ao carregar historico do arquivo {}: {}", ARQUIVO, e.getMessage());
        }
    }

    public void registrar(String tipo, String arquivo, long totalLinhas, ProcessamentoResult result) {

        String nomeRelatorio = result.getCaminhoRelatorio().getFileName().toString();
        String nomeExcel     = result.getCaminhoExcel().getFileName().toString();

        HistoricoImportacao entrada = HistoricoImportacao.builder()
                .id(result.getImportId())
                .tipo(tipo)
                .arquivo(arquivo)
                .dataHora(DateUtils.agora())
                .status(result.getStatusGeral())
                .processados(totalLinhas)
                .sucesso(result.getTotalSucesso())
                .erro(result.getTotalErro())
                .duplicado(result.getTotalDuplicado())
                .tempoMs(result.getTempoTotalMs())
                .downloadUrl("/download/" + nomeRelatorio)
                .excelUrl("/download/" + nomeExcel)
                .build();

        historico.add(0, entrada);
        persistir();
    }

    public List<HistoricoImportacao> listar() {
        return Collections.unmodifiableList(historico);
    }

    public Optional<HistoricoImportacao> buscarPorId(String id) {
        return historico.stream()
                .filter(h -> h.getId().equals(id))
                .findFirst();
    }

    private synchronized void persistir() {
        try {
            Files.createDirectories(ARQUIVO.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(ARQUIVO.toFile(), historico);
        } catch (Exception e) {
            log.error("Erro ao persistir historico em {}: {}", ARQUIVO, e.getMessage());
        }
    }
}
