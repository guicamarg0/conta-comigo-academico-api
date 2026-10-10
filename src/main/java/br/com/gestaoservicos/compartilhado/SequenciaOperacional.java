package br.com.gestaoservicos.compartilhado;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.UUID;

@Service
public class SequenciaOperacional {
    private final EntityManager entidades;
    private final JdbcTemplate jdbc;
    public SequenciaOperacional(EntityManager entidades, JdbcTemplate jdbc) { this.entidades=entidades; this.jdbc=jdbc; }
    @Transactional(propagation = Propagation.MANDATORY)
    public String proximo(UUID unidadeId, String prefixo) {
        String tabela = switch(prefixo) { case "CT" -> "contrato"; case "SV" -> "servico"; case "PC" -> "item_catalogo"; default -> throw new IllegalArgumentException("Prefixo inválido"); };
        String coluna = prefixo.equals("CT") ? "numero" : prefixo.equals("PC") ? "codigo_referencia" : "codigo";
        entidades.find(Unidade.class, unidadeId, LockModeType.PESSIMISTIC_WRITE);
        Long numero = jdbc.query("select proximo_numero from sequencia_operacional where unidade_id=? and prefixo=?", rs -> rs.next() ? rs.getLong(1) : null, unidadeId, prefixo);
        if (numero == null) {
            long maior=0;
            for (String existente : jdbc.queryForList("select "+coluna+" from "+tabela+" where unidade_id=?", String.class, unidadeId)) {
                if (existente != null && existente.matches(prefixo+"-[0-9]{1,18}")) maior=Math.max(maior,Long.parseLong(existente.substring(prefixo.length()+1)));
            }
            numero=maior+1;
            jdbc.update("insert into sequencia_operacional(unidade_id,prefixo,proximo_numero) values(?,?,?)",unidadeId,prefixo,numero+1);
        } else jdbc.update("update sequencia_operacional set proximo_numero=? where unidade_id=? and prefixo=?",numero+1,unidadeId,prefixo);
        return prefixo+"-"+numero;
    }
}
