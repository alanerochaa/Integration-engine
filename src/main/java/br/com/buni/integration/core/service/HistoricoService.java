package br.com.buni.integration.core.service;

import br.com.buni.integration.core.domain.importacao.RegistroImportacao;
import br.com.buni.integration.core.domain.importacao.TipoImportacao;
import br.com.buni.integration.core.model.dto.HistoricoImportacao;
import br.com.buni.integration.core.model.dto.ProcessamentoResult;
import br.com.buni.integration.core.repository.ImportacaoRepository;
import br.com.buni.integration.core.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final ImportacaoRepository repository;

    public void registrar(TipoImportacao tipo, String arquivo, long totalLinhas, ProcessamentoResult result) {

        String nomeRelatorio = result.getCaminhoRelatorio().getFileName().toString();
        String nomeExcel     = result.getCaminhoExcel().getFileName().toString();
        String base          = baseUrl.stripTrailing().replaceAll("/$", "");

        RegistroImportacao registro = RegistroImportacao.builder()
                .id(result.getImportId())
                .tipo(tipo)
                .nomeArquivoOriginal(arquivo)
                .nomeRelatorio(nomeRelatorio)
                .nomeExcel(nomeExcel)
                .dataHora(DateUtils.agora())
                .status(result.getStatusGeral())
                .totalProcessados(totalLinhas)
                .totalSucesso(result.getTotalSucesso())
                .totalErro(result.getTotalErro())
                .totalDuplicado(result.getTotalDuplicado())
                .tempoProcessamentoMs(result.getTempoTotalMs())
                .downloadUrl(base + "/download/" + nomeRelatorio)
                .excelUrl(base + "/download/" + nomeExcel)
                .build();

        repository.salvar(registro);
        log.info("[HISTORICO] Registro salvo — id={} | tipo={} | arquivo={} | status={}",
                registro.getId(), tipo, arquivo, result.getStatusGeral());
    }

    public List<HistoricoImportacao> listar() {
        return repository.listarTodos().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<HistoricoImportacao> buscarPorId(String id) {
        return repository.buscarPorId(id).map(this::toDto);
    }

    // ─── mapeamento domínio → DTO de API ────────────────────────────────────────

    private HistoricoImportacao toDto(RegistroImportacao r) {
        return HistoricoImportacao.builder()
                .id(r.getId())
                .tipo(r.getTipo() != null ? r.getTipo().name() : null)
                .arquivo(r.getNomeArquivoOriginal())
                .dataHora(r.getDataHora())
                .status(r.getStatus())
                .processados(r.getTotalProcessados())
                .sucesso(r.getTotalSucesso())
                .erro(r.getTotalErro())
                .duplicado(r.getTotalDuplicado())
                .tempoMs(r.getTempoProcessamentoMs())
                .downloadUrl(r.getDownloadUrl())
                .excelUrl(r.getExcelUrl())
                .build();
    }
}
