package br.com.buni.integration.core.templates.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class TemplateDto {
    private final String id;
    private final String nome;
    private final String tipo;
    private final List<String> formatos;
}
