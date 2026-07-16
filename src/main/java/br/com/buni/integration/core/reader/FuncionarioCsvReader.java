package br.com.buni.integration.core.reader;

import br.com.buni.integration.core.model.importrow.FuncionarioImportRow;
import org.springframework.stereotype.Component;

@Component
public class FuncionarioCsvReader extends AbstractCsvReader<FuncionarioImportRow> {

    public FuncionarioCsvReader() {
        super(FuncionarioImportRow.class);
    }
}
