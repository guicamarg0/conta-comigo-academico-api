package br.com.gestaoservicos.unidade.dto;

import br.com.gestaoservicos.associacao.model.Perfil;

import java.util.UUID;

public record RotaProtegidaResponseDTO(UUID unidadeId, Perfil perfil, String mensagem) {}
