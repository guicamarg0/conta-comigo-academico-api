package br.com.gestaoservicos.modelocontrato.dto;

import jakarta.validation.constraints.*;

public record ModeloContratoRequestDTO(@NotBlank @Size(max = 120) String nome,
    @Size(max = 500) String descricao, @NotBlank @Size(max = 30000) String conteudo) { }
