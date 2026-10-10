package br.com.gestaoservicos.convite.dto;

import br.com.gestaoservicos.associacao.model.Perfil;
import jakarta.validation.constraints.NotNull;

public record CriarConviteUnidadeRequestDTO(@NotNull Perfil perfil) {}
