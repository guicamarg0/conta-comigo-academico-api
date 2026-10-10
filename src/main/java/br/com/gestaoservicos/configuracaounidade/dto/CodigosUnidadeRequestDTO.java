package br.com.gestaoservicos.configuracaounidade.dto;
import jakarta.validation.constraints.*;
public record CodigosUnidadeRequestDTO(
    @NotBlank @Pattern(regexp="[A-Z][A-Z0-9]{0,9}", message="Use de 1 a 10 letras maiúsculas ou números, começando por uma letra.") String prefixoOrcamento,
    @Min(1) @Max(8) int digitosOrcamento,
    @NotBlank @Pattern(regexp="[A-Z][A-Z0-9]{0,9}", message="Use de 1 a 10 letras maiúsculas ou números, começando por uma letra.") String prefixoPeca,
    @NotNull @Pattern(regexp="[-_.]{0,3}", message="Use até três separadores: hífen, ponto ou sublinhado.") String separadorPeca,
    @Min(1) @Max(8) int digitosPeca) {}
