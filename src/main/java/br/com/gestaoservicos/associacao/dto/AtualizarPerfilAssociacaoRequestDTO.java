package br.com.gestaoservicos.associacao.dto;

import br.com.gestaoservicos.associacao.model.Perfil;
import jakarta.validation.constraints.NotNull;

public record AtualizarPerfilAssociacaoRequestDTO(
        @NotNull(message = "informe o perfil") Perfil perfil) {}
