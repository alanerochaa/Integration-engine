package br.com.buni.integration.core.service;

import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import br.com.buni.integration.core.reader.CsvReader;
import br.com.buni.integration.core.reader.ExcelReader;
import br.com.buni.integration.core.reader.FuncionarioCsvReader;
import br.com.buni.integration.core.reader.FuncionarioExcelReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileImportService {

    private static final Set<String> EXTENSOES_ACEITAS = Set.of("csv", "xls", "xlsx");

    private final CsvReader csvReader;
    private final ExcelReader excelReader;
    private final FuncionarioCsvReader funcionarioCsvReader;
    private final FuncionarioExcelReader funcionarioExcelReader;

    public List<ClienteImportRow> lerClientes(MultipartFile file) {
        String extensao = resolverExtensao(file);
        return switch (extensao) {
            case "csv"         -> csvReader.read(file);
            case "xls", "xlsx" -> excelReader.read(file);
            default -> throw unsupportedFormat(extensao);
        };
    }

    public List<FuncionarioImportRow> lerFuncionarios(MultipartFile file) {
        String extensao = resolverExtensao(file);
        return switch (extensao) {
            case "csv"         -> funcionarioCsvReader.read(file);
            case "xls", "xlsx" -> funcionarioExcelReader.read(file);
            default -> throw unsupportedFormat(extensao);
        };
    }

    private String resolverExtensao(MultipartFile file) {
        String nome = file.getOriginalFilename();

        if (nome == null || nome.isBlank() || !nome.contains(".")) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível determinar o formato do arquivo. Formatos aceitos: " + EXTENSOES_ACEITAS
            );
        }

        String extensao = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        if (!EXTENSOES_ACEITAS.contains(extensao)) {
            throw unsupportedFormat(extensao);
        }

        return extensao;
    }

    private ResponseStatusException unsupportedFormat(String extensao) {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Formato não suportado: '" + extensao + "'. Formatos aceitos: " + EXTENSOES_ACEITAS
        );
    }
}
