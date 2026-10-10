package br.com.gestaoservicos.servico.repository;

import br.com.gestaoservicos.servico.model.Servico;
import br.com.gestaoservicos.servico.model.StatusServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    Optional<Servico> findByIdAndUnidade_Id(UUID id, UUID unidadeId);

    @Query("select s from Servico s join s.contratante c where s.unidade.id = :unidadeId " +
           "and (:status is null or s.status = :status) and " +
           "(lower(s.codigo) like lower(concat('%', :busca, '%')) or " +
           "lower(s.titulo) like lower(concat('%', :busca, '%')) or " +
           "lower(c.nomeRazaoSocial) like lower(concat('%', :busca, '%')))")
    Page<Servico> buscar(UUID unidadeId, String busca, StatusServico status, Pageable pageable);

    @Query("select s from Servico s where s.unidade.id = :unidadeId and s.dataProgramada < :ate " +
           "and coalesce(s.dataFim, s.dataProgramada) >= :de and (:responsavel is null or s.responsavel = :responsavel) " +
           "and s.status <> br.com.gestaoservicos.servico.model.StatusServico.CANCELADO")
    Page<Servico> agenda(UUID unidadeId, java.time.LocalDate de, java.time.LocalDate ate, String responsavel, Pageable pageable);

}
