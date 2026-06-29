package br.com.buni.integration.core.enums;

public enum EstadoCivilEnum {

    NAO_DEFINIDO("0"),
    SOLTEIRO("1"),
    CASADO("2"),
    DESQUITADO("3"),
    DIVORCIADO("4"),
    VIUVO("5"),
    OUTROS("9");

    private final String codigo;

    EstadoCivilEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}