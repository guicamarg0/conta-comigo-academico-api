package br.com.gestaoservicos.onboarding.service;

import br.com.gestaoservicos.onboarding.dto.*;
import br.com.gestaoservicos.onboarding.repository.OnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Service
public class OnboardingService {
    private final OnboardingRepository repository;
    public OnboardingService(OnboardingRepository repository) { this.repository = repository; }
    @Transactional
    public OnboardingResponseDTO atualizar(UUID unidade, UUID usuario) { return repository.atualizar(unidade, usuario); }
    @Transactional
    public OnboardingResponseDTO salvar(UUID unidade, UUID usuario, OnboardingRequestDTO dados) {
        var atual = repository.atualizar(unidade, usuario);
        if (dados.conclusaoDispensada() && !atual.etapasConcluidas().stream().allMatch(Boolean::booleanValue))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A preparação da unidade ainda não foi concluída.");
        repository.salvarPreferencias(unidade, usuario, dados.apresentacaoVista(), dados.conclusaoDispensada());
        return repository.atualizar(unidade, usuario);
    }
}
