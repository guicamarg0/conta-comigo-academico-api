package br.com.gestaoservicos.unidade.dto;

import br.com.gestaoservicos.associacao.model.Perfil;

import java.util.UUID;

public record UnidadeResponseDTO(UUID id, String nome, Perfil perfil) {}
