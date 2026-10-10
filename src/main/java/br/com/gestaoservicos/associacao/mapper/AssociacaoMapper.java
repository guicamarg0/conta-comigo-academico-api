package br.com.gestaoservicos.associacao.mapper;

import br.com.gestaoservicos.associacao.dto.UsuarioUnidadeResponseDTO;
import br.com.gestaoservicos.associacao.model.Associacao;
import org.springframework.stereotype.Component;

@Component
public class AssociacaoMapper {
    public UsuarioUnidadeResponseDTO paraResponseDTO(Associacao associacao) {
        return new UsuarioUnidadeResponseDTO(associacao.getId(), associacao.getUsuario().getId(),
                associacao.getUsuario().getNome(), associacao.getUsuario().getEmail(), associacao.getPerfil(), associacao.isAtiva());
    }
}
