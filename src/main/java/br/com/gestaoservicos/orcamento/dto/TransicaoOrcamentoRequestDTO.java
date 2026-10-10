package br.com.gestaoservicos.orcamento.dto; import jakarta.validation.constraints.Size; public record TransicaoOrcamentoRequestDTO(@Size(max=2000) String observacao) { }
