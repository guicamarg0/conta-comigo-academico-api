package br.com.gestaoservicos.painel.repository;

import br.com.gestaoservicos.painel.dto.PainelResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Repository
public class PainelRepository {
    private final JdbcTemplate banco;
    public PainelRepository(JdbcTemplate banco) { this.banco = banco; }
    public PainelResponseDTO consultar(UUID unidade, LocalDate de, LocalDate ate) {
        var zona = ZoneId.of("America/Sao_Paulo");
        Timestamp inicio = Timestamp.from(de.atStartOfDay(zona).toInstant()), fim = Timestamp.from(ate.plusDays(1).atStartOfDay(zona).toInstant());
        String base = " from orcamento o join revisao_orcamento r on r.orcamento_id=o.id and r.numero_revisao=o.revisao_atual "
            + " where o.unidade_id=? and o.criado_em>=? and o.criado_em<? ";
        var totais = banco.queryForMap("select count(*) quantidade, coalesce(sum(r.total_final),0) valor, "
            + "coalesce(sum(case when o.status='APROVADO' then 1 else 0 end),0) aprovados, "
            + "coalesce(sum(case when o.status='APROVADO' then r.total_final else 0 end),0) valor_aprovados" + base, unidade, inicio, fim);
        var recentes = banco.query("select o.id,o.numero,coalesce(c.nome_fantasia,c.nome_razao_social,'Sem cliente') cliente,o.status,r.total_final "
            + "from orcamento o join revisao_orcamento r on r.orcamento_id=o.id and r.numero_revisao=o.revisao_atual "
            + "left join contratante c on c.id=r.contratante_id where o.unidade_id=? and o.criado_em>=? and o.criado_em<? order by o.criado_em desc limit 5",
            (rs,n)->new PainelResponseDTO.OrcamentoRecente(rs.getObject("id",UUID.class),rs.getString("numero"),rs.getString("cliente"),rs.getString("status"),rs.getBigDecimal("total_final")),unidade,inicio,fim);
        var pontos = banco.query("select o.criado_em,o.status,r.total_final" + base,
            (rs,n)->new PainelResponseDTO.Ponto(rs.getTimestamp("criado_em").toInstant().atZone(zona).toLocalDate(),1,rs.getBigDecimal("total_final"),
                "APROVADO".equals(rs.getString("status"))?rs.getBigDecimal("total_final"):BigDecimal.ZERO),unidade,inicio,fim);
        Map<LocalDate,PainelResponseDTO.Ponto> porDia = new TreeMap<>();
        for (var p:pontos) porDia.merge(p.dia(),p,(a,b)->new PainelResponseDTO.Ponto(a.dia(),a.quantidade()+1,a.proposto().add(b.proposto()),a.aprovado().add(b.aprovado())));
        List<PainelResponseDTO.Categoria> categorias = List.of();
        var clientesAtivos = banco.query("select coalesce(c.nome_fantasia,c.nome_razao_social) nome,count(*) quantidade,sum(r.total_final) valor "
            + "from orcamento o join revisao_orcamento r on r.orcamento_id=o.id and r.numero_revisao=o.revisao_atual join contratante c on c.id=r.contratante_id "
            + "where o.unidade_id=? and o.criado_em>=? and o.criado_em<? group by c.id,c.nome_fantasia,c.nome_razao_social order by quantidade desc limit 5",
            (rs,n)->new PainelResponseDTO.ClienteAtivo(rs.getString("nome"),rs.getLong("quantidade"),rs.getBigDecimal("valor")),unidade,inicio,fim);
        return new PainelResponseDTO(((Number)totais.get("quantidade")).longValue(),(BigDecimal)totais.get("valor"),((Number)totais.get("aprovados")).longValue(),(BigDecimal)totais.get("valor_aprovados"),
            contar("select count(*) from servico where unidade_id=? and status='EM_ANDAMENTO'",unidade),
            BigDecimal.ZERO,
            contar("select count(*) from contrato where unidade_id=? and status='VIGENTE' and ativo=true",unidade),contar("select count(*) from contratante where unidade_id=? and ativo=true",unidade),
            contar("select count(*) from item_catalogo where unidade_id=? and ativo=true",unidade),recentes,new ArrayList<>(porDia.values()),categorias,clientesAtivos);
    }
    private long contar(String sql, UUID unidade) { return banco.queryForObject(sql,Long.class,unidade); }
}
