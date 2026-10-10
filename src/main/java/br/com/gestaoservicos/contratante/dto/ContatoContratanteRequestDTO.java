package br.com.gestaoservicos.contratante.dto;

import jakarta.validation.constraints.*;

public record ContatoContratanteRequestDTO(@NotBlank @Size(max = 120) String nome, @Size(max = 30) String telefone, @Email @Size(max = 254) String email) {
    @AssertTrue(message = "informe telefone ou e-mail") public boolean possuiMeioContato() { return (telefone != null && !telefone.isBlank()) || (email != null && !email.isBlank()); }
}
