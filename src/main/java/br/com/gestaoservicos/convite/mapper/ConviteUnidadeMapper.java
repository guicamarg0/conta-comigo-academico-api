package br.com.gestaoservicos.convite.mapper;

import br.com.gestaoservicos.convite.dto.ConviteUnidadeResponseDTO;
import br.com.gestaoservicos.convite.model.ConviteUnidade;
import org.springframework.stereotype.Component;

@Component
public class ConviteUnidadeMapper {
    public ConviteUnidadeResponseDTO paraResponseDTO(ConviteUnidade convite, String codigo) {
        return new ConviteUnidadeResponseDTO(convite.getId(), convite.getPerfil(), convite.getExpiraEm(),
                convite.estaDisponivel(), codigo);
    }
}
