package br.com.buni.integration.core.report;

import br.com.buni.integration.core.model.dto.ExecutionReportLine;
import br.com.buni.integration.core.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ImportacaoReportGenerator {

    @Value("${app.report.output-dir:output}")
    private String outputDir;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Path gerarRelatorio(String nomeArquivo, List<ExecutionReportLine> linhas) {
        try {
            Path dir = Path.of(outputDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String base       = StringUtils.sanitizarNomeArquivo(nomeArquivo);
            String reportName = "relatorio_" + System.currentTimeMillis() + "_" + base + ".html";
            Path   caminho    = dir.resolve(reportName);

            try (BufferedWriter w = new BufferedWriter(
                    new FileWriter(caminho.toFile(), StandardCharsets.UTF_8))) {
                w.write(buildHtml(nomeArquivo, linhas));
            }

            log.info("[HTML] Relatório gerado — path={} | tamanho={} bytes",
                    caminho, Files.size(caminho));
            return caminho;
        } catch (Exception e) {
            log.error("[HTML] Falha ao gerar relatório — outputDir={} | erro={}",
                    outputDir, e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
        }
    }

    // ─── builder ────────────────────────────────────────────────────────────────

    private String buildHtml(String nomeArquivo, List<ExecutionReportLine> linhas) {
        long sucesso   = linhas.stream().filter(l -> "SUCESSO".equals(l.getStatus())).count();
        long erro      = linhas.stream().filter(l -> "ERRO".equals(l.getStatus())).count();
        long duplicado = linhas.stream().filter(l -> "DUPLICADO".equals(l.getStatus())).count();
        String geradoEm = FMT.format(LocalDateTime.now());

        StringBuilder sb = new StringBuilder(32_768);
        head(sb);
        headerBar(sb, nomeArquivo, geradoEm);
        summary(sb, linhas.size(), sucesso, erro, duplicado);
        table(sb, linhas);
        footerBar(sb);
        return sb.toString();
    }

    private void head(StringBuilder sb) {
        sb.append("""
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Relatório de Importação — B.uni</title>
            <style>
            *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
            html{background:#e8edf1;font-size:13px;font-family:"Segoe UI",system-ui,Arial,sans-serif;-webkit-font-smoothing:antialiased}
            body{color:#263846;min-height:100vh;display:flex;flex-direction:column;line-height:1.5}
            a{color:inherit;text-decoration:none}
            /* ── header ── */
            header{background:linear-gradient(135deg,#0c2340 0%,#0f5f7e 100%);padding:14px 28px;display:flex;align-items:center;justify-content:space-between;gap:20px}
            .brand{display:flex;align-items:center;gap:14px;flex-shrink:0}
            .logo{width:88px;height:auto;display:block;filter:brightness(0) invert(1)}
            .brand-sep{width:1px;height:22px;background:rgba(255,255,255,.3);flex-shrink:0}
            .brand-title{font-size:13px;font-weight:600;color:rgba(255,255,255,.9)}
            .header-meta{text-align:right;font-size:11px;color:rgba(255,255,255,.65);line-height:1.8}
            .header-meta strong{color:#fff;font-weight:600}
            /* ── main ── */
            main{max-width:1200px;width:100%;margin:22px auto;padding:0 24px;flex:1;display:grid;gap:16px;align-content:start}
            /* ── report title ── */
            .report-title{display:flex;align-items:center;gap:10px;padding-bottom:14px;border-bottom:1px solid #d7e5ec}
            .report-title h1{font-size:15px;font-weight:700;color:#00507A}
            .report-title svg{width:18px;height:18px;color:#005E91;flex-shrink:0}
            /* ── summary ── */
            .summary{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;align-items:start}
            .metric{background:#fff;border:1px solid #d7e5ec;border-radius:5px;padding:12px 16px;border-top:3px solid #005E91;box-shadow:0 1px 4px rgba(0,80,122,.06)}
            .metric.s{border-top-color:#14823b}
            .metric.e{border-top-color:#e0001a}
            .metric.d{border-top-color:#a66a00}
            .metric .lbl{font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.65px;color:#6d7d8b;margin-bottom:5px}
            .metric .val{font-size:22px;font-weight:700;font-variant-numeric:tabular-nums;color:#263846;line-height:1}
            .metric.s .val{color:#14823b}
            .metric.e .val{color:#e0001a}
            .metric.d .val{color:#a66a00}
            /* ── card / table ── */
            .card{background:#fff;border:1px solid #d7e5ec;border-radius:5px;overflow:hidden;box-shadow:0 1px 4px rgba(0,80,122,.07)}
            .card-hd{padding:13px 18px;border-bottom:1px solid #d7e5ec;display:flex;align-items:center;gap:8px}
            .card-hd h2{font-size:13px;font-weight:700;color:#00507A}
            table{width:100%;border-collapse:collapse}
            thead th{background:#f0f6fa;padding:10px 12px;text-align:left;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.5px;color:#6d7d8b;border-bottom:2px solid #d7e5ec;white-space:nowrap}
            tbody tr:nth-child(even){background:#fafcfe}
            tbody tr:hover{background:#edf5fa}
            td{padding:9px 12px;border-bottom:1px solid #edf5fa;font-size:11.5px;vertical-align:top;max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
            td.wrap{white-space:normal;word-break:break-word}
            /* ── badges ── */
            .badge{display:inline-block;padding:2px 9px;border-radius:20px;font-size:10px;font-weight:700;letter-spacing:.4px;text-transform:uppercase;white-space:nowrap}
            .bs{background:#f0faf4;color:#0b5224;border:1px solid #b6dfc6}
            .be{background:#fff0f2;color:#a50013;border:1px solid #fcc5cc}
            .bd{background:#fff8ed;color:#7a4f00;border:1px solid #f5ddb0}
            /* ── footer ── */
            footer{display:flex;align-items:center;justify-content:space-between;padding:13px 28px;border-top:1px solid #d7e5ec;background:#fff;color:#6d7d8b;font-size:11px;margin-top:auto;gap:12px}
            .fl a{color:#005E91;font-weight:700}
            .fr{display:flex;align-items:center;gap:5px}
            @media(max-width:700px){
              .summary{grid-template-columns:repeat(2,1fr)}
              header{flex-direction:column;align-items:flex-start;gap:8px}
              .header-meta{text-align:left}
              footer{flex-direction:column;gap:6px;text-align:center}
            }
            </style>
            </head>
            """);
    }

    private void headerBar(StringBuilder sb, String nomeArquivo, String geradoEm) {
        sb.append("<body>\n<header>\n")
          .append("  <div class=\"brand\">")
          .append("<img src=\"/images/logo-buni.png\" alt=\"B.uni\" class=\"logo\">")
          .append("<div class=\"brand-sep\"></div>")
          .append("<div class=\"brand-title\">Portal de Importação</div>")
          .append("</div>\n")
          .append("  <div class=\"header-meta\">")
          .append("Arquivo: <strong>").append(esc(nomeArquivo)).append("</strong><br>")
          .append("Gerado em: <strong>").append(geradoEm).append("</strong>")
          .append("</div>\n")
          .append("</header>\n<main>\n")
          .append("<div class=\"report-title\">")
          .append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\">")
          .append("<path d=\"M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z\"/><polyline points=\"14 2 14 8 20 8\"/>")
          .append("<line x1=\"8\" y1=\"13\" x2=\"16\" y2=\"13\"/><line x1=\"8\" y1=\"17\" x2=\"16\" y2=\"17\"/>")
          .append("</svg>")
          .append("<h1>Relatório de Importação</h1>")
          .append("</div>\n");
    }

    private void summary(StringBuilder sb, int total, long s, long e, long d) {
        sb.append("<div class=\"summary\">\n")
          .append(metricBox("",  "Total Processado", total))
          .append(metricBox("s", "Sucessos",         s))
          .append(metricBox("e", "Erros",            e))
          .append(metricBox("d", "Duplicados",       d))
          .append("</div>\n");
    }

    private String metricBox(String cls, String label, long value) {
        String extra = cls.isBlank() ? "" : " " + cls;
        return "<div class=\"metric" + extra + "\"><div class=\"lbl\">" + label
                + "</div><div class=\"val\">" + value + "</div></div>\n";
    }

    private void table(StringBuilder sb, List<ExecutionReportLine> linhas) {
        sb.append("""
            <div class="card">
              <div class="card-hd"><h2>Detalhamento por Linha</h2></div>
              <table>
                <thead>
                  <tr>
                    <th>Linha</th>
                    <th>CPF</th>
                    <th>Nome</th>
                    <th>Status</th>
                    <th>Cód. Cliente</th>
                    <th>Mensagem / Motivo</th>
                    <th>Ação Sugerida</th>
                    <th>ms</th>
                  </tr>
                </thead>
                <tbody>
            """);

        for (ExecutionReportLine l : linhas) {
            String badgeCls = switch (l.getStatus() != null ? l.getStatus() : "") {
                case "SUCESSO"   -> "badge bs";
                case "ERRO"      -> "badge be";
                default          -> "badge bd";
            };
            String mensagem = (l.getMotivoErro() != null && !l.getMotivoErro().isBlank())
                    ? l.getMotivoErro()
                    : v(l.getMensagemApi());

            sb.append("    <tr>")
              .append(td(v(l.getLinhaCsv())))
              .append(td(v(l.getCpf())))
              .append(td(esc(v(l.getNome()))))
              .append("<td><span class=\"").append(badgeCls).append("\">")
                  .append(v(l.getStatus())).append("</span></td>")
              .append(td(v(l.getCodigoCliente())))
              .append("<td class=\"wrap\" title=\"").append(esc(mensagem)).append("\">")
                  .append(esc(mensagem)).append("</td>")
              .append("<td class=\"wrap\" title=\"").append(esc(v(l.getAcaoSugerida()))).append("\">")
                  .append(esc(v(l.getAcaoSugerida()))).append("</td>")
              .append(td(v(l.getTempoProcessamentoMs())))
              .append("</tr>\n");
        }

        sb.append("        </tbody>\n      </table>\n    </div>\n</main>\n");
    }

    private void footerBar(StringBuilder sb) {
        sb.append("""
            <footer>
              <div class="fl">Desenvolvido por <a href="#">Catarse Tecnologia &amp; Consultoria</a></div>
              <div class="fr">&#128274; Ambiente seguro &middot; dados protegidos conforme a LGPD</div>
            </footer>
            </body>
            </html>
            """);
    }

    // ─── utilitários ────────────────────────────────────────────────────────────

    private static String td(String content) {
        return "<td>" + content + "</td>";
    }

    private static String v(Object val) {
        return val == null ? "" : val.toString();
    }

    private static String esc(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
