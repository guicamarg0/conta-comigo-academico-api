package br.com.gestaoservicos.usuario.dto;


import java.util.UUID;

public record UsuarioResponseDTO(UUID id, String nome, String email) {}
