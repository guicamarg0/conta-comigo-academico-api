package br.com.gestaoservicos.contrato.dto;
import java.time.Instant;
import java.util.UUID;
public record ContratoSnapshotResponseDTO(UUID id, int numeroVersao, String conteudo, Instant criadoEm, UUID usuarioId, String autor) { }
