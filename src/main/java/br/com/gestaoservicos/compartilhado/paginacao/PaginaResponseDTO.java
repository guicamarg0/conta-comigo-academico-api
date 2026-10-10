package br.com.gestaoservicos.compartilhado.paginacao;

import org.springframework.data.domain.Page;

import java.util.List;

/** Contrato de paginação reutilizável para listas de recursos da API. */
public record PaginaResponseDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponseDTO<T> de(Page<T> pagina) {
        return new PaginaResponseDTO<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
