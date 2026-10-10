package br.com.gestaoservicos.associacao.dto;
import br.com.gestaoservicos.associacao.model.Perfil; import jakarta.validation.constraints.*;
public record CriarUsuarioUnidadeRequestDTO(@NotBlank @Size(max=120) String nome, @NotBlank @Email String email, @NotBlank @Size(min=8,max=72) String senha, @NotNull Perfil perfil) {}
