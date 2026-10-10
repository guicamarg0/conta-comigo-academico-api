create table sessao_renovacao (
    id uuid primary key,
    usuario_id uuid not null references usuario(id),
    familia_id uuid not null,
    token_hash varchar(64) not null unique,
    versao_sessao integer not null,
    persistente boolean not null,
    criada_em timestamp with time zone not null,
    expira_em timestamp with time zone not null,
    rotacionada_em timestamp with time zone,
    substituida_por_id uuid references sessao_renovacao(id),
    revogada_em timestamp with time zone,
    motivo_revogacao varchar(40)
);

create index ix_sessao_renovacao_usuario on sessao_renovacao(usuario_id);
create index ix_sessao_renovacao_familia on sessao_renovacao(familia_id);
create index ix_sessao_renovacao_expiracao on sessao_renovacao(expira_em);
