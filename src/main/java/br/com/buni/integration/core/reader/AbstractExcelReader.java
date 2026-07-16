package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.util.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class AbstractExcelReader<T> implements ImportFileReader<T> {

    protected abstract T createRow();

    protected abstract Map<String, BiConsumer<T, String>> columnSetters();

    @Override
    public List<T> read(MultipartFile file) {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return Collections.emptyList();

            Map<Integer, String> indexParaColuna = new HashMap<>();
            for (Cell cell : headerRow) {
                String nomeColuna = StringUtils.removerAcentos(formatter.formatCellValue(cell).trim()).toUpperCase();
                if (!nomeColuna.isBlank()) {
                    indexParaColuna.put(cell.getColumnIndex(), nomeColuna);
                }
            }

            Map<String, BiConsumer<T, String>> setters = columnSetters();
            List<T> resultado = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || linhaVazia(row, formatter)) continue;

                T item = createRow();

                for (Cell cell : row) {
                    String nomeColuna = indexParaColuna.get(cell.getColumnIndex());
                    if (nomeColuna == null) continue;

                    BiConsumer<T, String> setter = setters.get(nomeColuna);
                    if (setter == null) continue;

                    String valor = formatter.formatCellValue(cell).trim();
                    setter.accept(item, valor);
                }

                resultado.add(item);
            }

            return resultado;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler arquivo Excel: " + e.getMessage(), e);
        }
    }

    private boolean linhaVazia(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isEmpty()) return false;
        }
        return true;
    }
}
