package br.com.gestaoservicos.compartilhado.paginacao;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaResponseDTOTest {
    @Test
    void preservaMetadadosDaPaginaDoSpringData() {
        var pagina = new PageImpl<>(List.of("primeiro", "segundo"), PageRequest.of(1, 2), 5);

        var resposta = PaginaResponseDTO.de(pagina);

        assertThat(resposta.conteudo()).containsExactly("primeiro", "segundo");
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.tamanho()).isEqualTo(2);
        assertThat(resposta.totalElementos()).isEqualTo(5);
        assertThat(resposta.totalPaginas()).isEqualTo(3);
    }
}
