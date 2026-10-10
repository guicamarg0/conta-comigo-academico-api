package br.com.gestaoservicos.contratante.dto;
import br.com.gestaoservicos.contratante.model.TipoContratante;
import java.util.*;
public record ContratanteResponseDTO(UUID id, TipoContratante tipo, String nomeRazaoSocial, String nomeFantasia, String documento, String endereco, String observacaoInterna, boolean ativo, List<ContatoContratanteResponseDTO> contatos) {}
