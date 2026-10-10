package br.com.gestaoservicos.orcamento.dto; import br.com.gestaoservicos.orcamento.model.TipoEventoOrcamento; import java.time.*; import java.util.*;
public record EventoOrcamentoResponseDTO(UUID id, UUID revisaoId, TipoEventoOrcamento tipo, String observacao, Instant ocorridoEm, UUID usuarioId) { }
