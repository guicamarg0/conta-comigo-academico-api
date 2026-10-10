package br.com.gestaoservicos.configuracaounidade.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record FormasPagamentoRequestDTO(@NotEmpty List<@Valid FormaRecebimentoRequestDTO> formasRecebimento) {}
