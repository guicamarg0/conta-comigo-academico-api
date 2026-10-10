package br.com.gestaoservicos.onboarding.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.gestaoservicos.onboarding.dto.OnboardingResponseDTO;
import java.util.UUID;
import java.util.stream.IntStream;

@Repository
public class OnboardingRepository {
    private final JdbcTemplate banco;
    public OnboardingRepository(JdbcTemplate banco) { this.banco = banco; }

    public OnboardingResponseDTO atualizar(UUID unidade, UUID usuario) {
        banco.queryForObject("select id from unidade where id=? for update", UUID.class, unidade);
        // Todos os critérios usam a unidade autenticada. Não dependem de listas paginadas do frontend.
        String[] criterios = {
            "select exists(select 1 from empresa where unidade_id=? and ativo=true)",
            "select exists(select 1 from forma_recebimento where unidade_id=? and ativa=true)",
            "select exists(select 1 from codigos_unidade where unidade_id=?)",
            "select exists(select 1 from modelo_contrato where unidade_id=? and status='PUBLICADO')",
            "select exists(select 1 from contratante where unidade_id=? and ativo=true) and exists(select 1 from item_catalogo where unidade_id=? and ativo=true)",
            "select exists(select 1 from orcamento where unidade_id=?)",
            "select exists(select 1 from contrato where unidade_id=?)",
            "select exists(select 1 from servico where unidade_id=?)"
        };
        int mascara = 0;
        for (int i=0; i<criterios.length; i++) {
            Object[] parametros = i==4 ? new Object[]{unidade, unidade} : new Object[]{unidade};
            if (Boolean.TRUE.equals(banco.queryForObject(criterios[i], Boolean.class, parametros))) mascara |= 1 << i;
        }
        var anteriores = banco.queryForList("select etapas_concluidas from onboarding_unidade where unidade_id=?", Integer.class, unidade);
        int salvo = mascara | (anteriores.isEmpty() ? 0 : anteriores.getFirst());
        if (anteriores.isEmpty()) banco.update("insert into onboarding_unidade (unidade_id, etapas_concluidas) values (?,?)", unidade, salvo);
        else if (salvo != anteriores.getFirst()) banco.update("update onboarding_unidade set etapas_concluidas=? where unidade_id=?", salvo, unidade);
        if (banco.queryForObject("select count(*) from onboarding_usuario where unidade_id=? and usuario_id=?", Integer.class, unidade, usuario)==0)
            banco.update("insert into onboarding_usuario (unidade_id, usuario_id) values (?,?)", unidade, usuario);
        return banco.queryForObject("select apresentacao_vista,conclusao_dispensada from onboarding_usuario where unidade_id=? and usuario_id=?",
            (rs,n)->new OnboardingResponseDTO(IntStream.range(0,8).mapToObj(i->(salvo & (1 << i))!=0).toList(),
                rs.getBoolean("apresentacao_vista"), rs.getBoolean("conclusao_dispensada")), unidade, usuario);
    }

    public void salvarPreferencias(UUID unidade, UUID usuario, boolean apresentacao, boolean dispensar) {
        banco.update("""
            update onboarding_usuario set apresentacao_vista = apresentacao_vista or ?,
            conclusao_dispensada = conclusao_dispensada or ? where unidade_id=? and usuario_id=?
            """, apresentacao, dispensar, unidade, usuario);
    }
}
