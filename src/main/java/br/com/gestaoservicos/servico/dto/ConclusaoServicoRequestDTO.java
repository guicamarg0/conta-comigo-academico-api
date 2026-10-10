package br.com.gestaoservicos.servico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConclusaoServicoRequestDTO(@NotBlank @Size(max = 4000) String resumoConclusao) {}
