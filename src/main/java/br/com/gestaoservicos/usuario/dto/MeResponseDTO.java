package br.com.gestaoservicos.usuario.dto;

import br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO;

import java.util.List;
import java.util.UUID;

public record MeResponseDTO(UUID id, String nome, String email, boolean administradorGeral, List<AssociacaoUnidadeResponseDTO> unidades) {}
