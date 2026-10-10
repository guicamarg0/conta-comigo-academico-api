package br.com.gestaoservicos.configuracaounidade.service;
import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.model.FormaRecebimento;
import br.com.gestaoservicos.configuracaounidade.repository.FormaRecebimentoRepository;
import br.com.gestaoservicos.configuracaounidade.mapper.ConfiguracaoUnidadeMapper;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class FormasPagamentoService {
    private final FormaRecebimentoRepository formas;
    private final ConfiguracaoUnidadeMapper mapper;
    private final EntityManager entidades;
    public FormasPagamentoService(FormaRecebimentoRepository formas, ConfiguracaoUnidadeMapper mapper, EntityManager entidades) {
        this.formas=formas; this.mapper=mapper; this.entidades=entidades;
    }
    @Transactional(readOnly=true)
    public FormasPagamentoResponseDTO consultar(UUID unidade) {
        return new FormasPagamentoResponseDTO(formas.findAllByUnidadeIdOrderByNomeExibicao(unidade).stream().map(mapper::paraResponseDTO).toList());
    }
    @Transactional
    public FormasPagamentoResponseDTO salvar(UUID unidadeId, FormasPagamentoRequestDTO dados) {
        var lista=dados.formasRecebimento();
        if (lista.stream().filter(f->f.ativa()&&f.padrao()).count()!=1 || lista.stream().anyMatch(f->f.padrao()&&!f.ativa()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escolha exatamente uma forma de pagamento padrão ativa.");
        var unidade=entidades.find(Unidade.class,unidadeId,LockModeType.PESSIMISTIC_WRITE);
        formas.deleteAllByUnidadeId(unidadeId); formas.flush();
        formas.saveAll(lista.stream().map(f->new FormaRecebimento(unidade,f.tipo(),f.nomeExibicao().strip(),f.instrucoes().strip(),f.ativa(),f.padrao())).toList());
        return consultar(unidadeId);
    }
}
