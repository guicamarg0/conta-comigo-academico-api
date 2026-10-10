package br.com.gestaoservicos.configuracaounidade.dto;

import java.util.List;

/** Dados copiados por uma revisão emitida; não deve ser reutilizado como referência mutável. */
public record DadosContratadoSnapshotDTO(String nomeRazaoSocial, String documento, String enderecoCompleto,
                                          String nomeFantasia, String email, String logoUrl, String responsavel,
                                          List<FormaRecebimentoResponseDTO> formasRecebimento) {}
