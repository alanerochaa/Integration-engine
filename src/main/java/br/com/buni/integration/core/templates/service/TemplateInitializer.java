package br.com.buni.integration.core.templates.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static br.com.buni.integration.core.templates.service.TemplateService.TEMPLATES_DIR;

@Component
@Slf4j
public class TemplateInitializer {

    private static final String[] HEADERS_CLIENTES = {
        "CNPJ", "NOMEFANTASIA", "CHAPA", "NOME", "DATAADMISSAO", "FUNCAO",
        "GRAUINSTRUCAO", "DTNASCIMENTO", "SITUACAO", "CPF", "SALARIO", "CEP",
        "RUA", "HORARIO", "NUMERO", "BANCO", "CODBANCOPAGTO", "CODAGENCIAPAGTO",
        "CONTAPAGAMENTO", "TELEFONE1", "TELEFONE2", "EMAILPROFISSIONAL",
        "EMAILPESSOAL", "SEXO", "CODSECAO"
    };

    private static final String[] HEADERS_FUNCIONARIOS = {
        "MATRICULA", "CPF", "NomeFuncionario", "NomePai", "NomeMae",
        "DATANASCIMENTO", "SEXO", "ESTADOCIVIL", "NATURALIDADE", "NACIONALIDADE",
        "CODNACIONALIDADE", "TIPODOCUMENTO", "RG", "ORGAOEMISSOR", "UFDOCUMENTO",
        "DATAEMISSAODOCUMENTO", "DATACADASTRO", "EMAIL", "DDDCEL", "CELULAR",
        "CEP", "LOGRADOURO", "NUMERO", "COMPLEMENTO", "BAIRRO", "CIDADE", "UF",
        "CARGO", "TPVINCULO", "BANCO", "AGENCIA", "CONTA", "DIGITOCONTA",
        "TIPODECONTA", "RENDABRUTA", "RENDALIQUIDA", "DATAADMISSAO", "SITUACAO",
        "CodOrg4", "CodOrg5", "DescOrg5"
    };

    @PostConstruct
    public void inicializar() {
        try {
            Files.createDirectories(TEMPLATES_DIR);
            gerar("Clientes",     HEADERS_CLIENTES);
            gerar("Funcionarios", HEADERS_FUNCIONARIOS);
            log.info("Templates verificados em: {}", TEMPLATES_DIR.toAbsolutePath());
        } catch (Exception ex) {
            log.error("Erro ao inicializar templates: {}", ex.getMessage());
        }
    }

    // ─── Geração condicional ──────────────────────────────────────────────────────

    private void gerar(String tipo, String[] headers) {
        gerarCsv(tipo, headers);
        gerarExcel(tipo, headers);
    }

    private void gerarCsv(String tipo, String[] headers) {

        Path destino = TEMPLATES_DIR.resolve("Modelo_Importacao_" + tipo + ".csv");
        if (Files.exists(destino)) return;

        try {
            String linha = String.join(";", headers);
            Files.writeString(destino, linha + System.lineSeparator(), StandardCharsets.UTF_8);
            log.info("Template CSV gerado: {}", destino.getFileName());
        } catch (Exception ex) {
            log.error("Erro ao gerar CSV {}: {}", destino.getFileName(), ex.getMessage());
        }
    }

    private void gerarExcel(String tipo, String[] headers) {

        Path destino = TEMPLATES_DIR.resolve("Modelo_Importacao_" + tipo + ".xlsx");
        if (Files.exists(destino)) return;

        try (XSSFWorkbook wb  = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(destino.toFile())) {

            XSSFSheet sheet = wb.createSheet("Modelo");

            // ── Linha 0: título ───────────────────────────────────────────────────
            XSSFRow titulo = sheet.createRow(0);
            titulo.setHeightInPoints(24);
            XSSFCell tc = titulo.createCell(0);
            tc.setCellValue("B.uni — Modelo de Importação: " + tipo);
            tc.setCellStyle(estiloTitulo(wb));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));

            // ── Linha 1: cabeçalho das colunas ───────────────────────────────────
            XSSFRow cabecalho = sheet.createRow(1);
            cabecalho.setHeightInPoints(18);
            XSSFCellStyle estCabecalho = estiloCabecalho(wb);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = cabecalho.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(estCabecalho);
                sheet.setColumnWidth(i, 22 * 256);
            }

            // ── Linha 2: exemplo em branco (estilo de dado) ───────────────────────
            XSSFRow exemplo = sheet.createRow(2);
            exemplo.setHeightInPoints(16);
            XSSFCellStyle estDado = estiloDado(wb);
            for (int i = 0; i < headers.length; i++) {
                exemplo.createCell(i).setCellStyle(estDado);
            }

            sheet.createFreezePane(0, 2);

            wb.write(fos);
            log.info("Template Excel gerado: {}", destino.getFileName());

        } catch (Exception ex) {
            log.error("Erro ao gerar Excel {}: {}", destino.getFileName(), ex.getMessage());
        }
    }

    // ─── Estilos ──────────────────────────────────────────────────────────────────

    private XSSFCellStyle estiloTitulo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(cor(wb, 0x0c, 0x23, 0x40));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        f.setColor(cor(wb, 0xff, 0xff, 0xff));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloCabecalho(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(cor(wb, 0x0f, 0x5f, 0x7e));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(cor(wb, 0xff, 0xff, 0xff));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloDado(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(cor(wb, 0xf8, 0xfa, 0xfc));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(cor(wb, 0x26, 0x38, 0x46));
        s.setFont(f);
        return s;
    }

    private XSSFColor cor(XSSFWorkbook wb, int r, int g, int b) {
        return new XSSFColor(
                new byte[]{(byte) r, (byte) g, (byte) b},
                wb.getStylesSource().getIndexedColors()
        );
    }
}
