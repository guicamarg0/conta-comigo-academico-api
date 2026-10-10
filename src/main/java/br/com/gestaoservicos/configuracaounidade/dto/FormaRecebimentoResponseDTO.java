package br.com.gestaoservicos.configuracaounidade.dto;

import br.com.gestaoservicos.configuracaounidade.model.TipoFormaRecebimento;
import java.util.UUID;

public record FormaRecebimentoResponseDTO(UUID id, TipoFormaRecebimento tipo, String nomeExibicao, String instrucoes, boolean ativa, boolean padrao) {}
