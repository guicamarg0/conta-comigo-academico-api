package br.com.gestaoservicos.contrato.dto;

import br.com.gestaoservicos.contrato.model.StatusContrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResponseDTO(UUID id, String numero, UUID contratanteId, String cliente,
        UUID orcamentoId, String numeroOrcamento, String modelo, String objeto,
        String clausulasAdicionais, LocalDate inicioVigencia, LocalDate fimVigencia,
        BigDecimal valorTotal, String condicoesPagamento, StatusContrato status, int numeroVersao,
        String assinadoPor, LocalDate assinadoEm, String canalAssinatura, String evidenciaUrl,
        UUID modeloContratoId, String conteudoRascunho, UUID empresaId, String empresaContratada, boolean ativo) {}
