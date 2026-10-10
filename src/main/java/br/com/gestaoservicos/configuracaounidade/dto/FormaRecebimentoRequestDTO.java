package br.com.gestaoservicos.configuracaounidade.dto;

import br.com.gestaoservicos.configuracaounidade.model.TipoFormaRecebimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FormaRecebimentoRequestDTO(
        @NotNull TipoFormaRecebimento tipo,
        @NotBlank @Size(max = 120) String nomeExibicao,
        @NotBlank @Size(max = 1000) String instrucoes,
        boolean ativa,
        boolean padrao) {}
