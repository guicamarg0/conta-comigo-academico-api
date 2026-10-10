package br.com.gestaoservicos.contrato.dto;
import br.com.gestaoservicos.contrato.model.TipoAtividadeContrato;
import java.time.Instant;
import java.util.UUID;
public record AtividadeContratoResponseDTO(UUID id, TipoAtividadeContrato tipo, String descricao, UUID usuarioId, String autor, Instant ocorridoEm) {}
