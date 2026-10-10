package br.com.gestaoservicos.associacao.dto;

import br.com.gestaoservicos.associacao.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdicionarUsuarioUnidadeRequestDTO(
        @NotBlank(message = "informe o e-mail") @Email(message = "informe um e-mail válido") String email,
        @NotNull(message = "informe o perfil") Perfil perfil) {}
