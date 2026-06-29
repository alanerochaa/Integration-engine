package br.com.buni.integration.core.enums;

public enum NacionalidadeEnum {

    NAO_DEFINIDA("0"),
    BRASILEIRA("1"),
    ESTRANGEIRA("2");

    private final String codigo;

    NacionalidadeEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}