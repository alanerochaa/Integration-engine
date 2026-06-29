package br.com.buni.integration.core.enums;

public enum EscolaridadeEnum {

    NAO_DEFINIDO("0"),
    PRIMEIRO_GRAU("1"),
    SEGUNDO_GRAU("2"),
    SUPERIOR("3"),
    SEM_ESCOLARIDADE("4"),
    POS_GRADUACAO("5"),
    MESTRADO_DOUTORADO("6");

    private final String codigo;

    EscolaridadeEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}