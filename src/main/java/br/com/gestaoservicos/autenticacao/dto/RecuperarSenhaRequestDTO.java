package br.com.gestaoservicos.autenticacao.dto;
import jakarta.validation.constraints.*;
public record RecuperarSenhaRequestDTO(@NotBlank @Email @Size(max = 254) String email) {}
