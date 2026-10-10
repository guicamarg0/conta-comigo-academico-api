package br.com.gestaoservicos.convite.dto;
import br.com.gestaoservicos.associacao.model.Perfil;
import java.time.Instant;
public record ConsultaConviteResponseDTO(String unidade, Perfil perfil, Instant expiraEm) {}
