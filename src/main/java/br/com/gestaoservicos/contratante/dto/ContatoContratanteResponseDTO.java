package br.com.gestaoservicos.contratante.dto;
import java.util.UUID;
public record ContatoContratanteResponseDTO(UUID id, String nome, String telefone, String email) {}
