package br.com.buni.integration.core.repository.impl;

import br.com.buni.integration.core.domain.importacao.RegistroImportacao;
import br.com.buni.integration.core.repository.ImportacaoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador de persistência em memória com escrita em arquivo JSON.
 *
 * Para substituir por banco de dados ou armazenamento em nuvem, basta
 * criar uma nova implementação de ImportacaoRepository e registrá-la
 * como @Primary ou remover esta.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class InMemoryImportacaoRepository implements ImportacaoRepository {

    @Value("${app.historico.arquivo:data/historico.json}")
    private String arquivoHistorico;

    private final ObjectMapper objectMapper;
    private final List<RegistroImportacao> registros = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void carregar() {
        Path arquivo = Path.of(arquivoHistorico).toAbsolutePath().normalize();
        try {
            if (!Files.exists(arquivo)) {
                log.info("[HISTORICO] Nenhum arquivo encontrado em {}. Iniciando sem histórico anterior.", arquivo);
                return;
            }
            List<RegistroImportacao> carregados = objectMapper.readValue(
                    arquivo.toFile(),
                    new TypeReference<List<RegistroImportacao>>() {}
            );
            registros.addAll(carregados);
            log.info("[HISTORICO] {} registro(s) carregado(s) de {}", registros.size(), arquivo);
        } catch (Exception e) {
            log.error("[HISTORICO] Erro ao carregar histórico de {} — a aplicação inicia sem registros anteriores. Causa: {}",
                    arquivo, e.getMessage(), e);
        }
    }

    @Override
    public void salvar(RegistroImportacao registro) {
        registros.add(0, registro);
        persistir();
    }

    @Override
    public List<RegistroImportacao> listarTodos() {
        return Collections.unmodifiableList(registros);
    }

    @Override
    public Optional<RegistroImportacao> buscarPorId(String id) {
        return registros.stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst();
    }

    private synchronized void persistir() {
        Path arquivo = Path.of(arquivoHistorico).toAbsolutePath().normalize();
        try {
            Files.createDirectories(arquivo.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(arquivo.toFile(), registros);
            log.debug("[HISTORICO] {} registro(s) persistido(s) em {}", registros.size(), arquivo);
        } catch (Exception e) {
            log.error("[HISTORICO] Erro ao persistir histórico em {}: {}", arquivo, e.getMessage(), e);
        }
    }
}
