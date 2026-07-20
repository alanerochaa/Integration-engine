package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.model.importrow.ClienteImportRow;
import org.springframework.stereotype.Component;

@Component
public class CsvReader extends AbstractCsvReader<ClienteImportRow> {

    public CsvReader() {
        super(ClienteImportRow.class);
    }
}