package br.com.buni.integration.core.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro persistente de CPFs de funcionários cadastrados com sucesso.
 * Persiste em arquivo texto (data/funcionarios-registrados.txt) e recarrega no startup.
 * Garante que o mesmo CPF não seja cadastrado novamente mesmo após reinicialização da aplicação.
 */
@Service
@Slf4j
public class FuncionarioCpfRegistry {

    private static final Path ARQUIVO = Path.of("data/funcionarios-registrados.txt");

    private final Map<String, String> cpfParaImportId = new ConcurrentHashMap<>();

    @PostConstruct
    public void carregar() {
        try {
            if (!Files.exists(ARQUIVO)) {
                Files.createDirectories(ARQUIVO.getParent());
                log.info("CPF Registry: nenhum histórico anterior encontrado. Iniciando vazio.");
                return;
            }
            Files.lines(ARQUIVO)
                    .filter(linha -> !linha.isBlank())
                    .map(linha -> linha.split(",", 2))
                    .filter(partes -> partes.length == 2)
                    .forEach(partes -> cpfParaImportId.put(partes[0].trim(), partes[1].trim()));

            log.info("CPF Registry carregado: {} CPFs de funcionários registrados.", cpfParaImportId.size());
        } catch (Exception e) {
            log.error("Erro ao carregar CPF Registry do arquivo {}: {}", ARQUIVO, e.getMessage());
        }
    }

    /**
     * Tenta reservar o CPF atomicamente.
     * Thread-safe: o bloco completo (verificação + inserção + persistência) é sincronizado.
     *
     * @return true se inserido com sucesso; false se o CPF já estava registrado
     */
    public synchronized boolean registrar(String cpf, String importId) {
        if (cpfParaImportId.containsKey(cpf)) {
            return false;
        }
        cpfParaImportId.put(cpf, importId);
        persistir(cpf, importId);
        return true;
    }

    /**
     * Remove o CPF do registro, permitindo nova tentativa de importação.
     * Usado quando validação ou chamada de API falha após o CPF ter sido reservado.
     */
    public synchronized void liberarRegistro(String cpf) {
        if (cpfParaImportId.remove(cpf) != null) {
            reescreverArquivo();
        }
    }

    public boolean jaExiste(String cpf) {
        return cpfParaImportId.containsKey(cpf);
    }

    public String importIdOrigem(String cpf) {
        return cpfParaImportId.get(cpf);
    }

    private void persistir(String cpf, String importId) {
        try {
            Files.writeString(
                    ARQUIVO,
                    cpf + "," + importId + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            log.error("Erro ao persistir CPF {} no registro: {}", cpf, e.getMessage());
        }
    }

    private void reescreverArquivo() {
        try {
            StringBuilder sb = new StringBuilder();
            cpfParaImportId.forEach((cpf, importId) ->
                sb.append(cpf).append(",").append(importId).append(System.lineSeparator())
            );
            Files.writeString(ARQUIVO, sb.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.error("Erro ao reescrever CPF Registry: {}", e.getMessage());
        }
    }
}
