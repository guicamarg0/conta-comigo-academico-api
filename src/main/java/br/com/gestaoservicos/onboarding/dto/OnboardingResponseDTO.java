package br.com.gestaoservicos.onboarding.dto;

import java.util.List;

public record OnboardingResponseDTO(List<Boolean> etapasConcluidas, boolean apresentacaoVista,
                                    boolean conclusaoDispensada) {}
