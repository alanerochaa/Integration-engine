package br.com.buni.integration.core.service;

import br.com.buni.integration.core.model.csv.ClienteCsv;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

@Service
public class CsvService {

    /**
     * Charset utilizado para leitura do arquivo CSV.
     * Padrão UTF-8. Alterar para ISO-8859-1 ou WINDOWS-1252 caso o arquivo
     * venha de sistemas de RH legados que exportam com encoding Latin.
     */
    @Value("${buni.csv.charset:UTF-8}")
    private String csvCharset;

    public List<ClienteCsv> lerClientes(MultipartFile file) {

        try (Reader reader = new InputStreamReader(file.getInputStream(), Charset.forName(csvCharset))) {

            return new CsvToBeanBuilder<ClienteCsv>(reader)
                    .withType(ClienteCsv.class)
                    .withSeparator(';')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build()
                    .parse();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler CSV: " + e.getMessage(), e);
        }
    }
}
