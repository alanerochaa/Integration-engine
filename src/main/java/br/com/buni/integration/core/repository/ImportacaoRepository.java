package br.com.buni.integration.core.repository;

import br.com.buni.integration.core.domain.importacao.RegistroImportacao;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para persistência de registros de importação.
 *
 * Implementações atuais: InMemoryImportacaoRepository (memória + JSON).
 * Futuras: JdbcImportacaoRepository, BlobImportacaoRepository, etc.
 */
public interface ImportacaoRepository {

    void salvar(RegistroImportacao registro);

    List<RegistroImportacao> listarTodos();

    Optional<RegistroImportacao> buscarPorId(String id);
}
