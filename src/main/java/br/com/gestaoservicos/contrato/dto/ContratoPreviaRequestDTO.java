package br.com.gestaoservicos.contrato.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ContratoPreviaRequestDTO(@NotNull UUID orcamentoId, @NotNull UUID modeloContratoId,
                                       @NotNull UUID empresaId, @Size(max = 30000) String conteudoRascunho) { }
