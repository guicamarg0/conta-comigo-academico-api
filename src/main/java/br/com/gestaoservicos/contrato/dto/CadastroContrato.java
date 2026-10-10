package br.com.gestaoservicos.contrato.dto;
public enum CadastroContrato {
    ATIVO, INATIVO, TODOS;
    public Boolean filtroAtivo() { return this == TODOS ? null : this == ATIVO; }
}
