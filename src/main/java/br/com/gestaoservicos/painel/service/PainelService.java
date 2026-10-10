package br.com.gestaoservicos.painel.service;
import br.com.gestaoservicos.painel.dto.PainelResponseDTO;
import br.com.gestaoservicos.painel.repository.PainelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
@Service
public class PainelService {
    private final PainelRepository repository;
    public PainelService(PainelRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public PainelResponseDTO consultar(UUID unidade, LocalDate de, LocalDate ate) {
        if (ate.isBefore(de) || ChronoUnit.DAYS.between(de,ate)>366) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um período de até 12 meses.");
        return repository.consultar(unidade,de,ate);
    }
}
