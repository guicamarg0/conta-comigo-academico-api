package br.com.gestaoservicos.orcamento.dto; import br.com.gestaoservicos.orcamento.model.StatusOrcamento; import java.util.*;
public record OrcamentoResponseDTO(UUID id,String numero,int revisaoAtual,StatusOrcamento status,RevisaoOrcamentoResponseDTO revisaoAtualDetalhe,List<RevisaoOrcamentoResponseDTO> revisoes,List<EventoOrcamentoResponseDTO> historico) { }
