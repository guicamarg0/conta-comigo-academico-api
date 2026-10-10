package br.com.gestaoservicos.unidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUnidadeRequestDTO(@NotBlank @Size(max = 120) String nome) {}
