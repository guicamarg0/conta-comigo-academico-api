package br.com.gestaoservicos.painel.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record PainelResponseDTO(long orcamentos, BigDecimal valorOrcamentos, long aprovados, BigDecimal valorAprovados,
    long servicosEmAndamento, BigDecimal despesasAprovadas, long contratosAtivos, long clientes, long itensCatalogo,
    List<OrcamentoRecente> recentes, List<Ponto> evolucao, List<Categoria> categorias, List<ClienteAtivo> clientesAtivos) {
    public record OrcamentoRecente(UUID id, String numero, String cliente, String status, BigDecimal valor) {}
    public record Ponto(LocalDate dia, long quantidade, BigDecimal proposto, BigDecimal aprovado) {}
    public record Categoria(String nome, long quantidade) {}
    public record ClienteAtivo(String nome, long quantidade, BigDecimal valor) {}
}
