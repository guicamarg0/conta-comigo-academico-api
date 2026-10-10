package br.com.gestaoservicos.autenticacao.dto;
import jakarta.validation.constraints.*;
public record RedefinirSenhaRequestDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
    @NotBlank @Size(min = 8, max = 72) String senha) {}
