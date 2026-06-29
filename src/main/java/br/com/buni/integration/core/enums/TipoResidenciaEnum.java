package br.com.buni.integration.core.enums;

public enum TipoResidenciaEnum {

    NAO_DEFINIDO("0"),
    PROPRIA("1"),
    ALUGADA("2"),
    FAMILIARES("3"),
    EMPRESA("4"),
    FINANCIADA("5"),
    HOTEL("6");

    private final String codigo;

    TipoResidenciaEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}