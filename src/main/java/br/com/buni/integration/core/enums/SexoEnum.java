package br.com.buni.integration.core.enums;

public enum SexoEnum {

    MASCULINO("1"),
    FEMININO("2");

    private final String codigo;

    SexoEnum(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}