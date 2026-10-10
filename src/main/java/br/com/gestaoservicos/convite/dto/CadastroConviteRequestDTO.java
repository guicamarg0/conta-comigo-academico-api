package br.com.gestaoservicos.convite.dto;
import jakarta.validation.constraints.*;
public record CadastroConviteRequestDTO(
        @NotBlank String codigo, @NotBlank @Size(max=120) String nome,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=72) String senha) {}
