package br.com.gestaoservicos.configuracaounidade.model;
import br.com.gestaoservicos.configuracaounidade.dto.*;
import br.com.gestaoservicos.unidade.model.Unidade;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="codigos_unidade")
public class CodigosUnidade {
    @Id @Column(name="unidade_id") private UUID unidadeId;
    @MapsId @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="unidade_id") private Unidade unidade;
    @Column(name="prefixo_orcamento",nullable=false,length=10) private String prefixoOrcamento="ORC";
    @Column(name="digitos_orcamento",nullable=false) private int digitosOrcamento=1;
    @Column(name="prefixo_peca",nullable=false,length=10) private String prefixoPeca="PC";
    @Column(name="separador_peca",nullable=false,length=3) private String separadorPeca="-";
    @Column(name="digitos_peca",nullable=false) private int digitosPeca=4;
    protected CodigosUnidade() {}
    public CodigosUnidade(Unidade unidade) { this.unidade=unidade; }
    public void atualizar(CodigosUnidadeRequestDTO dto) { prefixoOrcamento=dto.prefixoOrcamento(); digitosOrcamento=dto.digitosOrcamento(); prefixoPeca=dto.prefixoPeca(); separadorPeca=dto.separadorPeca(); digitosPeca=dto.digitosPeca(); }
    public CodigosUnidadeResponseDTO resposta() { return new CodigosUnidadeResponseDTO(prefixoOrcamento,digitosOrcamento,prefixoPeca,separadorPeca,digitosPeca); }
}
