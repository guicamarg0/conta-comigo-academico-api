package br.com.gestaoservicos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class IntegridadeUnidadeIntegrationTest {
    @Autowired JdbcTemplate banco;

    @Test
    void bancoRejeitaClienteDeOutraUnidadeMesmoSemPassarPelaApi() {
        UUID primeira = unidade(), segunda = unidade(), cliente = UUID.randomUUID();
        banco.update("insert into contratante(id,unidade_id,tipo,nome_razao_social,documento,ativo,criado_em,atualizado_em) values (?,?,'PF','Cliente fictício','12345678901',true,current_timestamp,current_timestamp)", cliente, primeira);
        UUID servico = UUID.randomUUID();
        String inserir = "insert into servico(id,unidade_id,contratante_id,codigo,titulo,status,criado_em,atualizado_em) values (?,?,?,'SV-1','Serviço fictício','RASCUNHO',current_timestamp,current_timestamp)";
        assertThatThrownBy(() -> banco.update(inserir, servico, segunda, cliente))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(banco.update(inserir, servico, primeira, cliente)).isEqualTo(1);
        assertThatThrownBy(() -> banco.update("update servico set unidade_id=? where id=?", segunda, servico))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(banco.queryForObject("select unidade_id from servico where id=?", UUID.class, servico)).isEqualTo(primeira);
    }

    private UUID unidade() {
        UUID id = UUID.randomUUID();
        banco.update("insert into unidade(id,nome,nome_normalizado,criada_em) values (?,?,?,current_timestamp)", id, "Teste " + id, "teste " + id);
        return id;
    }
}
