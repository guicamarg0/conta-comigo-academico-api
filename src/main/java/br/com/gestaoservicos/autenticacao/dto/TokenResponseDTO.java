package br.com.gestaoservicos.autenticacao.dto;

import java.time.Instant;

public record TokenResponseDTO(String tokenAcesso, String tipoToken, Instant expiraEm) {}
