package br.com.gestaoservicos.catalogo.repository;

import br.com.gestaoservicos.catalogo.model.ItemCatalogo;
import br.com.gestaoservicos.catalogo.model.TipoItemCatalogo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, UUID> {
    @Query("select i from ItemCatalogo i where i.unidade.id = :unidadeId and (:tipo is null or i.tipo = :tipo) and (:ativo is null or i.ativo = :ativo) and (lower(i.nome) like lower(concat('%', :busca, '%')) or lower(coalesce(i.codigoReferencia, '')) like lower(concat('%', :busca, '%')))")
    Page<ItemCatalogo> buscar(UUID unidadeId, String busca, TipoItemCatalogo tipo, Boolean ativo, Pageable pageable);
    boolean existsByUnidadeIdAndCodigoReferencia(UUID unidadeId, String codigoReferencia);
    Optional<ItemCatalogo> findByIdAndUnidadeId(UUID id, UUID unidadeId);
}
