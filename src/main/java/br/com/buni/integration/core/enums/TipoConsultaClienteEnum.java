package br.com.buni.integration.core.enums;

public enum TipoConsultaClienteEnum {

    CLIENTE("0"),
    PROPONENTE("1");

    private final String codigo;

    TipoConsultaClienteEnum(
            String codigo
    ) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}