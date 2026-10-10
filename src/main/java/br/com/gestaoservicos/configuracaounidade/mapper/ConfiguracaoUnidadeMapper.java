package br.com.gestaoservicos.configuracaounidade.mapper;

import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.model.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConfiguracaoUnidadeMapper {
    public ConfiguracaoUnidadeResponseDTO paraResponseDTO(ConfiguracaoUnidade configuracao, List<FormaRecebimento> formas) {
        return new ConfiguracaoUnidadeResponseDTO(configuracao.getNomeRazaoSocial(), configuracao.getDocumento(), configuracao.getEnderecoCompleto(), configuracao.getNomeFantasia(), configuracao.getEmail(), configuracao.getLogoUrl(), configuracao.getResponsavel(), configuracao.getCondicoesPagamentoPadrao(), formas.stream().map(this::paraResponseDTO).toList());
    }
    public FormaRecebimentoResponseDTO paraResponseDTO(FormaRecebimento forma) { return new FormaRecebimentoResponseDTO(forma.getId(), forma.getTipo(), forma.getNomeExibicao(), forma.getInstrucoes(), forma.isAtiva(), forma.isPadrao()); }
    public DadosContratadoSnapshotDTO paraSnapshot(ConfiguracaoUnidade configuracao, List<FormaRecebimento> formas) {
        return new DadosContratadoSnapshotDTO(configuracao.getNomeRazaoSocial(), configuracao.getDocumento(), configuracao.getEnderecoCompleto(), configuracao.getNomeFantasia(), configuracao.getEmail(), configuracao.getLogoUrl(), configuracao.getResponsavel(), formas.stream().filter(FormaRecebimento::isAtiva).map(this::paraResponseDTO).toList());
    }
}
