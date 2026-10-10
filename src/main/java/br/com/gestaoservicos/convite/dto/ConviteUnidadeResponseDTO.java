package br.com.gestaoservicos.convite.dto;

import br.com.gestaoservicos.associacao.model.Perfil;
import java.time.Instant;
import java.util.UUID;

public record ConviteUnidadeResponseDTO(UUID id, Perfil perfil, Instant expiraEm,
                                         boolean disponivel, String codigoConvite) {}
