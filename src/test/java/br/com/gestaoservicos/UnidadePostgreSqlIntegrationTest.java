package br.com.gestaoservicos;

import br.com.gestaoservicos.unidade.model.Unidade;
import br.com.gestaoservicos.unidade.repository.UnidadeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class UnidadePostgreSqlIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired UnidadeRepository unidades;

    @Test
    void aplicaMigracaoEImpedeNomeNormalizadoDuplicadoNoPostgreSql() {
        unidades.saveAndFlush(new Unidade("Unidade PostgreSQL", "unidade postgresql"));

        assertThatThrownBy(() -> unidades.saveAndFlush(
                new Unidade("UNIDADE POSTGRESQL", "unidade postgresql")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
