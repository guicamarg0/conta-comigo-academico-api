package br.com.gestaoservicos.orcamento.dto;

import br.com.gestaoservicos.orcamento.model.StatusOrcamento;
import java.util.List;
import java.util.UUID;

/** Prévia sem efeitos persistentes; itens manuais não integram a lista. */
public record AtualizacaoCatalogoPreviaResponseDTO(
        UUID orcamentoId,
        StatusOrcamento statusAtual,
        boolean criaraNovaRevisao,
        List<MudancaValorCatalogoResponseDTO> mudancas) {
}
