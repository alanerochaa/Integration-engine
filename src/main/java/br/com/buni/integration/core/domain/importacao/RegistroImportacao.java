package br.com.buni.integration.core.domain.importacao;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Entidade de domínio que representa uma importação executada.
 *
 * Campos com @JsonAlias mantêm compatibilidade de leitura com o formato
 * anterior do historico.json (que usava nomes mais curtos como "arquivo",
 * "processados", "sucesso", etc.).
 */
@Getter
@Builder
@Jacksonized
public class RegistroImportacao {

    private final String id;
    private final TipoImportacao tipo;

    @JsonAlias("arquivo")
    private final String nomeArquivoOriginal;

    private final String nomeRelatorio;
    private final String nomeExcel;
    private final String dataHora;
    private final String status;

    @JsonAlias("processados")
    private final long totalProcessados;

    @JsonAlias("sucesso")
    private final long totalSucesso;

    @JsonAlias("erro")
    private final long totalErro;

    @JsonAlias("duplicado")
    private final long totalDuplicado;

    @JsonAlias("tempoMs")
    private final long tempoProcessamentoMs;

    private final String downloadUrl;
    private final String excelUrl;
}
