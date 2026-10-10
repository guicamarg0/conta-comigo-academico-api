package br.com.gestaoservicos.configuracaounidade.service;
import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.configuracaounidade.model.CodigosUnidade;
import br.com.gestaoservicos.configuracaounidade.repository.CodigosUnidadeRepository;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@Service
public class CodigosUnidadeService {
    private final CodigosUnidadeRepository codigos;
    private final EntityManager entidades;
    public CodigosUnidadeService(CodigosUnidadeRepository codigos, EntityManager entidades) { this.codigos=codigos; this.entidades=entidades; }
    @Transactional(readOnly=true)
    public CodigosUnidadeResponseDTO consultar(UUID unidadeId) { return codigos.findById(unidadeId).map(CodigosUnidade::resposta).orElse(new CodigosUnidadeResponseDTO("ORC",1,"PC","-",4)); }
    @Transactional
    public CodigosUnidadeResponseDTO salvar(UUID unidadeId, CodigosUnidadeRequestDTO dto) {
        var unidade=entidades.find(Unidade.class,unidadeId,LockModeType.PESSIMISTIC_WRITE);
        if(unidade==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unidade não encontrada.");
        var configuracao=codigos.findById(unidadeId).orElseGet(()->new CodigosUnidade(unidade));
        configuracao.atualizar(dto);
        return codigos.save(configuracao).resposta();
    }
    public String formatar(UUID unidadeId, String tipo, long numero) {
        var config=consultar(unidadeId);
        String prefixo=tipo.equals("ORC")?config.prefixoOrcamento():config.prefixoPeca();
        String separador=tipo.equals("ORC")?"-":config.separadorPeca();
        int digitos=tipo.equals("ORC")?config.digitosOrcamento():config.digitosPeca();
        return prefixo+separador+String.format(java.util.Locale.ROOT,"%0"+digitos+"d",numero);
    }
}
