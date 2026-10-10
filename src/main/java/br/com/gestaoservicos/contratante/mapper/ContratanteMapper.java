package br.com.gestaoservicos.contratante.mapper;
import br.com.gestaoservicos.contratante.dto.*;
import br.com.gestaoservicos.contratante.model.*;
import org.springframework.stereotype.Component;
@Component public class ContratanteMapper {
    public ContatoContratante paraEntidade(ContatoContratanteRequestDTO dto, String telefone, String email) { return new ContatoContratante(dto.nome().strip(), telefone, email); }
    public ContratanteResponseDTO paraResponseDTO(Contratante c) { return new ContratanteResponseDTO(c.getId(), c.getTipo(), c.getNomeRazaoSocial(), c.getNomeFantasia(), c.getDocumento(), c.getEndereco(), c.getObservacaoInterna(), c.isAtivo(), c.getContatos().stream().map(x -> new ContatoContratanteResponseDTO(x.getId(), x.getNome(), x.getTelefone(), x.getEmail())).toList()); }
}
