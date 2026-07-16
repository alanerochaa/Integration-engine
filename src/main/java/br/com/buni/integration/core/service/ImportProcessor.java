package br.com.buni.integration.core.service;

import br.com.buni.integration.core.model.dto.ProcessamentoResult;

import java.util.List;

public interface ImportProcessor<T> {

    ProcessamentoResult processar(String nomeArquivo, List<T> registros);
}
