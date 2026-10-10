package br.com.gestaoservicos.configuracaounidade.dto;

import java.util.List;

public record ConfiguracaoUnidadeResponseDTO(String nomeRazaoSocial, String documento, String enderecoCompleto,
                                               String nomeFantasia, String email, String logoUrl, String responsavel,
                                               String condicoesPagamentoPadrao, List<FormaRecebimentoResponseDTO> formasRecebimento) {}
