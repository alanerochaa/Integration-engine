package br.com.buni.integration.core.enums;

public enum TipoDocumentoEnum {

    RG("RG"),
    RNE("RNE"),
    CTPS("CTPS"),
    CNH("CNH"),
    CR("CR"),
    CIN("CIN");

    private final String codigo;

    TipoDocumentoEnum(String codigo){
        this.codigo = codigo;
    }

    public String getCodigo(){
        return codigo;
    }
}