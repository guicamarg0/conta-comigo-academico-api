package br.com.gestaoservicos.unidade.dto;

import jakarta.validation.constraints.*;

public record CriarUnidadeGeralRequestDTO(
        @NotBlank @Size(max=120) String nome,
        @NotBlank @Size(max=120) String nomeAdministrador,
        @NotBlank @Email @Size(max=254) String emailAdministrador,
        @Size(min=8,max=72) String senhaAdministrador) {}
