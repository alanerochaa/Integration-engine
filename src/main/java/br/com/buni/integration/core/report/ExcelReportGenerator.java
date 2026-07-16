package br.com.buni.integration.core.report;

import br.com.buni.integration.core.model.dto.ExecutionReportLine;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelReportGenerator {

    private static final String OUTPUT_DIR = "output";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String[] HEADERS = {
        "Data / Hora", "CPF", "Nome", "Status",
        "Código Cliente", "Mensagem / Motivo", "Ação Sugerida"
    };
    private static final int[] COL_WIDTHS = { 20, 16, 30, 13, 20, 44, 36 };

    // ─── public ──────────────────────────────────────────────────────────────────

    public Path gerarExcel(String nomeArquivo, List<ExecutionReportLine> linhas) {
        try {
            Files.createDirectories(Path.of(OUTPUT_DIR));
            String base       = limparNomeArquivo(nomeArquivo);
            String reportName = "relatorio_" + System.currentTimeMillis() + "_" + base + ".xlsx";
            Path   caminho    = Path.of(OUTPUT_DIR, reportName);

            try (XSSFWorkbook wb  = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(caminho.toFile())) {
                build(wb, linhas);
                wb.write(fos);
            }
            return caminho;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Excel: " + e.getMessage(), e);
        }
    }

    // ─── montagem ────────────────────────────────────────────────────────────────

    private void build(XSSFWorkbook wb, List<ExecutionReportLine> linhas) {
        XSSFSheet sheet = wb.createSheet("Relatório de Importação");

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, COL_WIDTHS[i] * 256);
        }

        // Linha 0 — título
        XSSFRow title = sheet.createRow(0);
        title.setHeightInPoints(26);
        XSSFCell tc = title.createCell(0);
        tc.setCellValue("B.uni — Relatório de Importação");
        tc.setCellStyle(titleStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        // Linha 1 — cabeçalho da tabela
        XSSFRow header = sheet.createRow(1);
        header.setHeightInPoints(18);
        XSSFCellStyle hs = headerStyle(wb);
        for (int i = 0; i < HEADERS.length; i++) cell(header, i, HEADERS[i], hs);

        sheet.createFreezePane(0, 2);

        // Linhas 2+ — dados
        int rowIdx = 2;
        for (ExecutionReportLine l : linhas) {
            XSSFRow row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(16);

            String dataHora = l.getDataHoraExecucao() != null
                    ? FMT.format(l.getDataHoraExecucao()) : "";
            String mensagem = (l.getMotivoErro() != null && !l.getMotivoErro().isBlank())
                    ? l.getMotivoErro() : v(l.getMensagemApi());

            XSSFCellStyle base  = dataStyle(wb, l.getStatus());
            XSSFCellStyle badge = badgeStyle(wb, l.getStatus());

            cell(row, 0, dataHora,                   base);
            cell(row, 1, v(l.getCpf()),              base);
            cell(row, 2, v(l.getNome()),             base);
            cell(row, 3, v(l.getStatus()),           badge);
            cell(row, 4, v(l.getCodigoCliente()),    base);
            cell(row, 5, mensagem,                   base);
            cell(row, 6, v(l.getAcaoSugerida()),     base);
        }
    }

    // ─── estilos ─────────────────────────────────────────────────────────────────

    private XSSFCellStyle titleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(rgb(wb, "0c2340"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        f.setColor(rgb(wb, "ffffff"));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(rgb(wb, "0f5f7e"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(rgb(wb, "ffffff"));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle dataStyle(XSSFWorkbook wb, String status) {
        XSSFCellStyle s = wb.createCellStyle();
        String bg = switch (status != null ? status : "") {
            case "SUCESSO"   -> "f0faf4";
            case "ERRO"      -> "fff0f2";
            case "DUPLICADO" -> "fff8ed";
            default          -> "ffffff";
        };
        s.setFillForegroundColor(rgb(wb, bg));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(rgb(wb, "263846"));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle badgeStyle(XSSFWorkbook wb, String status) {
        XSSFCellStyle s = wb.createCellStyle();
        String bg, fg;
        switch (status != null ? status : "") {
            case "SUCESSO"   -> { bg = "f0faf4"; fg = "0b5224"; }
            case "ERRO"      -> { bg = "fff0f2"; fg = "a50013"; }
            default          -> { bg = "fff8ed"; fg = "7a4f00"; }
        }
        s.setFillForegroundColor(rgb(wb, bg));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(rgb(wb, fg));
        s.setFont(f);
        return s;
    }

    // ─── utilitários ─────────────────────────────────────────────────────────────

    private void cell(XSSFRow row, int col, String val, XSSFCellStyle style) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(style);
    }

    private XSSFColor rgb(XSSFWorkbook wb, String hex) {
        return new XSSFColor(hexBytes(hex), wb.getStylesSource().getIndexedColors());
    }

    private byte[] hexBytes(String hex) {
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private String v(Object val) { return val == null ? "" : val.toString(); }

    private String limparNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) return "arquivo";
        nome = nome.replace("\\", "_").replace("/", "_").replace(" ", "_");
        while (nome.toLowerCase().endsWith(".csv")) nome = nome.substring(0, nome.length() - 4);
        // Substitui qualquer caractere que não seja alfanumérico, hífen ou underscore
        // para evitar ".." (path traversal) e outros padrões bloqueados por segurança
        nome = nome.replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return nome.isBlank() ? "arquivo" : nome;
    }
}
