package br.com.gestaoservicos.contratante.dto;

import br.com.gestaoservicos.contratante.model.TipoContratante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ContratanteRequestDTO(@NotNull TipoContratante tipo, @NotBlank @Size(max = 120) String nomeRazaoSocial, @Size(max = 120) String nomeFantasia, @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "informe um CPF ou CNPJ com apenas números") String documento, @Size(max = 500) String endereco, @Size(max = 2000) String observacaoInterna, @NotEmpty List<@Valid ContatoContratanteRequestDTO> contatos) {}
