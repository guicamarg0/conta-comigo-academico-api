package br.com.gestaoservicos.modelocontrato.dto;

import java.time.Instant;
import java.util.UUID;
public record VersaoModeloContratoResponseDTO(UUID id, int numero, String conteudo, Instant criadoEm) { }
