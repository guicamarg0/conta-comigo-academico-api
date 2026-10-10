package br.com.gestaoservicos.modelocontrato.dto;

import br.com.gestaoservicos.modelocontrato.model.StatusModeloContrato;
import java.time.Instant;
import java.util.UUID;

public record ModeloContratoResponseDTO(UUID id, String nome, String descricao, String conteudo,
    StatusModeloContrato status, int numeroVersao, Instant atualizadoEm) { }
