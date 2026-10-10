package br.com.gestaoservicos.contrato.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoRequestDTO(@NotNull UUID orcamentoId,
        @NotBlank @Size(max = 120) String modelo,
        @NotBlank @Size(max = 4000) String objeto,
        @Size(max = 8000) String clausulasAdicionais,
        @NotNull LocalDate inicioVigencia, @NotNull LocalDate fimVigencia,
        UUID modeloContratoId, @Size(max = 30000) String conteudoRascunho, UUID empresaId) {}
