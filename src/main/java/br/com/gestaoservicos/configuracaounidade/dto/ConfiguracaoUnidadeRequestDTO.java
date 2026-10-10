package br.com.gestaoservicos.configuracaounidade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConfiguracaoUnidadeRequestDTO(
        @NotBlank @Size(max = 120) String nomeRazaoSocial,
        @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "informe um CPF ou CNPJ com apenas números") String documento,
        @NotBlank @Size(max = 500) String enderecoCompleto,
        @Size(max = 120) String nomeFantasia,
        @Email @Size(max = 254) String email,
        @Size(max = 500) String logoUrl,
        @Size(max = 120) String responsavel,
        @Size(max = 2000) String condicoesPagamentoPadrao,
        @NotEmpty List<@Valid FormaRecebimentoRequestDTO> formasRecebimento) {}
