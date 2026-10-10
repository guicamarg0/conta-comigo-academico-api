package br.com.gestaoservicos.unidade.mapper;

import br.com.gestaoservicos.associacao.dto.UsuarioUnidadeResponseDTO;
import br.com.gestaoservicos.associacao.model.Associacao;
import br.com.gestaoservicos.associacao.model.Perfil;
import br.com.gestaoservicos.unidade.dto.AssociacaoUnidadeResponseDTO;
import br.com.gestaoservicos.unidade.dto.UnidadeResponseDTO;
import br.com.gestaoservicos.unidade.model.Unidade;
import org.springframework.stereotype.Component;

@Component
public class UnidadeMapper {
    public UnidadeResponseDTO paraResponseDTO(Unidade unidade, Perfil perfil) {
        return new UnidadeResponseDTO(unidade.getId(), unidade.getNome(), perfil);
    }

    public AssociacaoUnidadeResponseDTO paraAssociacaoResponseDTO(Associacao associacao) {
        return new AssociacaoUnidadeResponseDTO(associacao.getUnidade().getId(),
                associacao.getUnidade().getNome(), associacao.getPerfil());
    }
}
