package br.com.buni.integration.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private static final Logger log =
            LoggerFactory.getLogger(LogService.class);

    public void inicioProcessamento(
            String nomeArquivo,
            int totalLinhas
    ) {
        log.info(
                "Iniciando processamento. Arquivo: {} | Total linhas: {}",
                nomeArquivo,
                totalLinhas
        );
    }

    public void sucessoLinha(
            int linha,
            String cpf,
            String nome
    ) {
        log.info(
                "Linha processada com sucesso. Linha: {} | CPF: {} | Nome: {}",
                linha,
                cpf,
                nome
        );
    }

    public void erroValidacao(
            int linha,
            String cpf,
            String erros
    ) {
        log.warn(
                "Erro de validação. Linha: {} | CPF: {} | Erros: {}",
                linha,
                cpf,
                erros
        );
    }

    public void erroApi(
            int linha,
            String cpf,
            String erro
    ) {
        log.error(
                "Erro na API. Linha: {} | CPF: {} | Erro: {}",
                linha,
                cpf,
                erro
        );
    }

    public void fimProcessamento(String nomeArquivo, String caminhoRelatorio,
                                  long totalMs, long sucesso, long erro, long duplicado) {
        log.info(
                "Processamento finalizado. Arquivo: {} | Tempo: {}ms | Sucesso: {} | Erro: {} | Duplicado: {} | Relatório: {}",
                nomeArquivo, totalMs, sucesso, erro, duplicado, caminhoRelatorio
        );
    }

    public void respostaApi(String cpf, int httpStatus, String resposta) {
        log.info(
                "Resposta API externa. CPF: {} | HTTP: {} | Resposta: {}",
                cpf, httpStatus, resposta
        );
    }
}