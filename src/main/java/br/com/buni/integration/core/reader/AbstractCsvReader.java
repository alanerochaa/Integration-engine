package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.util.StringUtils;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;

public abstract class AbstractCsvReader<T> implements ImportFileReader<T> {

    @Value("${buni.csv.charset:UTF-8}")
    private String csvCharset;

    private final Class<T> type;

    protected AbstractCsvReader(Class<T> type) {
        this.type = type;
    }

    @Override
    public List<T> read(MultipartFile file) {
        try {
            Charset charset = Charset.forName(csvCharset);

            String conteudo = new String(file.getBytes(), charset);

            if (!conteudo.isEmpty() && conteudo.charAt(0) == '﻿') {
                conteudo = conteudo.substring(1);
            }

            conteudo = normalizarHeaderAcentos(conteudo);

            char separador = descobrirSeparador(conteudo);

            try (StringReader reader = new StringReader(conteudo)) {
                return new CsvToBeanBuilder<T>(reader)
                        .withType(type)
                        .withSeparator(separador)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreEmptyLine(true)
                        .build()
                        .parse();
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler CSV: " + e.getMessage(), e);
        }
    }

    private String normalizarHeaderAcentos(String conteudo) {
        int fimHeader = conteudo.indexOf('\n');
        if (fimHeader < 0) {
            return StringUtils.removerAcentos(conteudo);
        }
        String header = StringUtils.removerAcentos(conteudo.substring(0, fimHeader));
        return header + conteudo.substring(fimHeader);
    }

    private char descobrirSeparador(String conteudo) {
        int fimHeader = conteudo.indexOf('\n');
        String header = fimHeader > 0 ? conteudo.substring(0, fimHeader) : conteudo;

        long pontoVirgula = header.chars().filter(c -> c == ';').count();
        long virgula      = header.chars().filter(c -> c == ',').count();

        return virgula > pontoVirgula ? ',' : ';';
    }
}
