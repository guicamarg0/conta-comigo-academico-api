package br.com.gestaoservicos.servico.dto;

import br.com.gestaoservicos.servico.model.StatusServico;
import java.time.Instant;
import java.util.UUID;

public record ServicoResponseDTO(UUID id, String codigo, UUID contratanteId, String cliente,
        UUID orcamentoId, String titulo, String descricao, String responsavel,
        String localExecucao, Instant inicioPrevisto, Instant fimPrevisto,
        Instant concluidoEm, String resumoConclusao, StatusServico status, java.time.LocalDate dataProgramada, java.time.LocalDate dataInicio, java.time.LocalDate dataFim) {}
