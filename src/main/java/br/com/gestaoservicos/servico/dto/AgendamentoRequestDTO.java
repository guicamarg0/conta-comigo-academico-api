package br.com.gestaoservicos.servico.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record AgendamentoRequestDTO(LocalDate dataProgramada, LocalDate dataInicio, LocalDate dataFim,
        @NotBlank @Size(max = 120) String responsavel) {}
