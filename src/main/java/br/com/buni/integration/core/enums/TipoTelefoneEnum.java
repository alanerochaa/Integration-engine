package br.com.buni.integration.core.enums;

public enum TipoTelefoneEnum {

    NAO_DEFINIDO("0"),
    FONE_FISICO("1"),
    COMERCIAL_1("2"),
    COMERCIAL_2("3"),
    CELULAR("4"),
    RECADO("5"),
    EMPREGO_ANTERIOR("6");

    private final String codigo;

    TipoTelefoneEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}