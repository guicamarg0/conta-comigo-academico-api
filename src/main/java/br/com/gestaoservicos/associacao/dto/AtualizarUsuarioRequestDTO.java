package br.com.gestaoservicos.associacao.dto;
import jakarta.validation.constraints.*;
import br.com.gestaoservicos.associacao.model.Perfil;
public record AtualizarUsuarioRequestDTO(@NotBlank @Size(max = 120) String nome,
    @NotBlank @Email @Size(max = 254) String email, @NotNull Perfil perfil) {}
