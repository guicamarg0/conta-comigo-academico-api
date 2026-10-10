package br.com.gestaoservicos.associacao.dto;

import br.com.gestaoservicos.associacao.model.Perfil;

import java.util.UUID;

public record UsuarioUnidadeResponseDTO(UUID id, UUID usuarioId, String nome, String email, Perfil perfil, boolean ativa) {}
