package br.com.gestaoservicos.convite.dto;
import jakarta.validation.constraints.NotBlank; public record AceitarConviteRequestDTO(@NotBlank String codigo) {}
