-- Conta Comigo: base inicial para banco vazio. Não aplicar sobre a base legada.

create table usuario (
    id uuid primary key,
    nome varchar(120) not null,
    email varchar(254) not null,
    senha_hash varchar(200) not null,
    criado_em timestamp with time zone not null,
    constraint uk_usuario_email unique (email),
    versao_sessao integer not null default 0, administrador_geral boolean not null default false
);

create table unidade (
    id uuid primary key,
    nome varchar(120) not null,
    nome_normalizado varchar(120) not null unique,
    criada_em timestamp with time zone not null
);

create table associacao (
    id uuid primary key,
    usuario_id uuid not null references usuario(id),
    unidade_id uuid not null references unidade(id),
    perfil varchar(20) not null,
    criada_em timestamp with time zone not null,
    constraint ck_associacao_perfil check (perfil in ('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA')),
    constraint uk_associacao_usuario_unidade unique (usuario_id, unidade_id),
    ativa boolean not null default true
);

create table convite_unidade (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    email varchar(254),
    perfil varchar(20) not null,
    token_hash varchar(64) not null unique,
    expira_em timestamp with time zone not null,
    aceita_em timestamp with time zone,
    revogada_em timestamp with time zone,
    criada_em timestamp with time zone not null,
    constraint ck_convite_unidade_perfil check (perfil in ('ADMIN', 'GESTOR', 'OPERADOR', 'CONSULTA'))
);

create table configuracao_unidade (
    id uuid primary key,
    unidade_id uuid not null unique references unidade(id),
    nome_razao_social varchar(120) not null,
    documento varchar(14) not null,
    endereco_completo varchar(500) not null,
    nome_fantasia varchar(120),
    email varchar(254),
    logo_url varchar(500),
    responsavel varchar(120),
    condicoes_pagamento_padrao varchar(2000)
);

create table forma_recebimento (
    id uuid primary key,
    configuracao_unidade_id uuid references configuracao_unidade(id),
    tipo varchar(20) not null,
    nome_exibicao varchar(120) not null,
    instrucoes varchar(1000) not null,
    ativa boolean not null,
    padrao boolean not null,
    constraint ck_forma_recebimento_tipo check (tipo in ('PIX', 'CONTA_BANCARIA', 'OUTRO')),
    unidade_id uuid not null references unidade(id)
);

create table contratante (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    tipo varchar(2) not null,
    nome_razao_social varchar(120) not null,
    nome_fantasia varchar(120),
    documento varchar(14) not null,
    endereco varchar(500),
    observacao_interna varchar(2000),
    ativo boolean not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint ck_contratante_tipo check (tipo in ('PF', 'PJ')),
    constraint uk_contratante_unidade_documento unique (unidade_id, documento)
);

create table contato_contratante (
    id uuid primary key,
    contratante_id uuid not null references contratante(id),
    nome varchar(120) not null,
    telefone varchar(30),
    email varchar(254),
    constraint ck_contato_contratante_meio check (telefone is not null or email is not null)
);

create table item_catalogo (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    tipo varchar(20) not null,
    nome varchar(120) not null,
    descricao varchar(2000),
    unidade_medida varchar(30) not null,
    unidade_medida_personalizada varchar(60),
    valor_padrao numeric(15, 2) not null,
    codigo_referencia varchar(80),
    ativo boolean not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint ck_item_catalogo_tipo check (tipo in ('MAO_DE_OBRA', 'PECA', 'MATERIAL')),
    constraint ck_item_catalogo_unidade_medida check (unidade_medida in ('UNIDADE', 'HORA', 'DIARIA', 'METRO', 'METRO_QUADRADO', 'QUILOGRAMA', 'LITRO', 'OUTRA')),
    constraint ck_item_catalogo_unidade_personalizada check ((unidade_medida = 'OUTRA' and unidade_medida_personalizada is not null) or (unidade_medida <> 'OUTRA' and unidade_medida_personalizada is null))
);

create table sequencia_orcamento (
    unidade_id uuid not null references unidade(id),
    ano integer not null,
    proximo_numero bigint not null,
    primary key (unidade_id, ano)
);

create table orcamento (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    numero varchar(30) not null,
    revisao_atual integer not null,
    status varchar(20) not null,
    emitido_inicialmente boolean not null default false,
    versao bigint not null default 0,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_orcamento_unidade_numero unique (unidade_id, numero),
    constraint ck_orcamento_status check (status in ('RASCUNHO', 'EMITIDO', 'APROVADO', 'RECUSADO', 'CANCELADO'))
);

create table revisao_orcamento (
    id uuid primary key,
    orcamento_id uuid not null references orcamento(id),
    numero_revisao integer not null,
    status varchar(20) not null,
    contratante_id uuid references contratante(id),
    validade date,
    condicoes_pagamento varchar(2000),
    observacoes_comerciais varchar(4000),
    exibir_assinatura boolean not null default false,
    desconto_tipo varchar(12) not null,
    desconto_valor numeric(15,2) not null default 0,
    acrescimo_tipo varchar(12) not null,
    acrescimo_valor numeric(15,2) not null default 0,
    subtotal_servicos numeric(15,2) not null default 0,
    subtotal_materiais numeric(15,2) not null default 0,
    subtotal numeric(15,2) not null default 0,
    desconto_total numeric(15,2) not null default 0,
    acrescimo_total numeric(15,2) not null default 0,
    total_final numeric(15,2) not null default 0,
    contratante_snapshot varchar(8000),
    contratado_snapshot text,
    pagamentos_snapshot varchar(8000),
    versao bigint not null default 0,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_revisao_orcamento unique (orcamento_id, numero_revisao),
    constraint ck_revisao_orcamento_status check (status in ('RASCUNHO', 'EMITIDO', 'APROVADO', 'RECUSADO', 'CANCELADO')),
    constraint ck_revisao_orcamento_desconto_tipo check (desconto_tipo in ('VALOR', 'PERCENTUAL')),
    constraint ck_revisao_orcamento_acrescimo_tipo check (acrescimo_tipo in ('VALOR', 'PERCENTUAL')),
    unidade_id uuid not null references unidade(id), empresa_id uuid, exibir_pagamento boolean not null default true, referencia varchar(4000)
);

create table item_revisao_orcamento (
    id uuid primary key,
    revisao_orcamento_id uuid not null references revisao_orcamento(id),
    ordem integer not null,
    item_catalogo_id uuid references item_catalogo(id),
    tipo varchar(20) not null,
    descricao varchar(2000) not null,
    unidade_medida varchar(30) not null,
    unidade_medida_personalizada varchar(60),
    quantidade numeric(15,2) not null,
    valor_unitario numeric(15,2) not null,
    desconto numeric(15,2) not null default 0,
    total numeric(15,2) not null,
    constraint uk_item_revisao_ordem unique (revisao_orcamento_id, ordem),
    constraint ck_item_revisao_tipo check (tipo in ('MAO_DE_OBRA', 'PECA', 'MATERIAL')),
    constraint ck_item_revisao_medida check (unidade_medida in ('UNIDADE', 'HORA', 'DIARIA', 'METRO', 'METRO_QUADRADO', 'QUILOGRAMA', 'LITRO', 'OUTRA')),
    unidade_id uuid not null references unidade(id)
);

create table evento_orcamento (
    id uuid primary key,
    orcamento_id uuid not null references orcamento(id),
    revisao_orcamento_id uuid references revisao_orcamento(id),
    tipo varchar(40) not null,
    observacao varchar(2000),
    ocorrido_em timestamp with time zone not null,
    usuario_id uuid
);

create table pdf_orcamento (
    id uuid primary key,
    revisao_orcamento_id uuid not null unique references revisao_orcamento(id),
    nome_arquivo varchar(160) not null,
    conteudo bytea,
    gerado_em timestamp with time zone not null,
    caminho_storage varchar(500)
);

create table servico (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    contratante_id uuid not null references contratante(id),
    orcamento_id uuid references orcamento(id),
    codigo varchar(30) not null,
    titulo varchar(200) not null,
descricao varchar(4000),
    responsavel varchar(120),
local_execucao varchar(500),
    inicio_previsto timestamp with time zone,
    fim_previsto timestamp with time zone,
    concluido_em timestamp with time zone,
    resumo_conclusao varchar(4000),
    status varchar(30) not null,
    versao bigint not null default 0,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_servico_codigo unique (unidade_id, codigo),
    constraint ck_servico_status check (status in ('RASCUNHO','AGENDADO','EM_ANDAMENTO','AGUARDANDO','CONCLUIDO','CANCELADO')),
    data_programada date, data_fim_programada date, constraint ck_servico_periodo check (data_fim_programada is null or (data_programada is not null and data_fim_programada >= data_programada))
);

create table contrato (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    contratante_id uuid not null references contratante(id),
    orcamento_id uuid not null references orcamento(id),
    numero varchar(30) not null,
    modelo varchar(120) not null,
    objeto varchar(4000) not null,
    clausulas_adicionais varchar(8000),
    inicio_vigencia date not null,
    fim_vigencia date not null,
    valor_total numeric(15,2) not null,
    condicoes_pagamento varchar(2000),
    status varchar(30) not null,
    numero_versao integer not null,
    assinado_por varchar(120),
    assinado_em date,
    canal_assinatura varchar(120),
    evidencia_url varchar(1000),
    versao bigint not null default 0,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_contrato_numero unique (unidade_id, numero),
    constraint uk_contrato_orcamento unique (orcamento_id),
    constraint ck_contrato_status check (status in ('AGUARDANDO_ASSINATURA','VIGENTE','ENCERRADO')),
    empresa_id uuid, modelo_contrato_id uuid, conteudo_rascunho text, ativo boolean not null default true
);

create table modelo_contrato (
    id uuid primary key,
    unidade_id uuid not null references unidade(id),
    nome varchar(120) not null,
    descricao varchar(500),
    conteudo text not null,
    status varchar(20) not null,
    numero_versao integer not null,
    versao bigint not null default 0,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_modelo_contrato_nome unique (unidade_id, nome),
    constraint ck_modelo_contrato_status check (status in ('RASCUNHO','PUBLICADO','ARQUIVADO'))
);

create table modelo_contrato_versao (
    id uuid primary key,
    modelo_contrato_id uuid not null references modelo_contrato(id),
    numero integer not null,
    conteudo text not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_modelo_contrato_versao unique (modelo_contrato_id, numero)
);

create table contrato_snapshot (
    id uuid primary key,
    contrato_id uuid not null references contrato(id),
    numero_versao integer not null,
    conteudo text not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    criado_por uuid,
    atualizado_por uuid,
    constraint uk_contrato_snapshot unique (contrato_id, numero_versao)
);

-- Histórico operacional separado das versões de conteúdo.
create table atividade_contrato (
    id uuid primary key,
    contrato_id uuid not null references contrato(id),
    tipo varchar(30) not null,
    descricao varchar(500) not null,
    usuario_id uuid references usuario(id),
    ocorrido_em timestamp with time zone not null,
    constraint ck_atividade_contrato_tipo check (tipo in
        ('CRIADO', 'EDITADO', 'RESTAURADO', 'VIGENTE', 'ENCERRADO', 'INATIVADO', 'REATIVADO'))
);
create index ix_atividade_contrato_historico on atividade_contrato(contrato_id, ocorrido_em, id);

create table empresa (
    id uuid primary key,
  unidade_id uuid not null references unidade(id),
  matriz_id uuid references empresa(id),
  tipo varchar(10) not null,
  razao_social varchar(160) not null,
  nome_fantasia varchar(160),
  cnpj varchar(14) not null,
  inscricao_estadual varchar(40),
  telefone varchar(30),
  email varchar(254),
  cep varchar(12),
  logradouro varchar(180),
  numero varchar(30),
  complemento varchar(120),
  bairro varchar(120),
  cidade varchar(120),
  estado varchar(2),
  ativo boolean not null default true,
  unique(unidade_id, cnpj),
    logo_url text
);

create table recuperacao_senha (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em TIMESTAMP WITH TIME ZONE
);

create table arquivo (
    id UUID PRIMARY KEY,
    unidade_id UUID NOT NULL REFERENCES unidade(id),
    nome VARCHAR(200) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    caminho VARCHAR(500),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    conteudo bytea, constraint ck_arquivo_conteudo_ou_storage check (conteudo is not null or caminho is not null)
);

create table sequencia_operacional (
    unidade_id UUID NOT NULL REFERENCES unidade(id),
 prefixo VARCHAR(10) NOT NULL,
 proximo_numero BIGINT NOT NULL CHECK (proximo_numero > 0),
 PRIMARY KEY (unidade_id, prefixo)
);

create table codigos_unidade (
    unidade_id uuid primary key references unidade(id),
    prefixo_orcamento varchar(10) not null default 'ORC',
    digitos_orcamento integer not null default 1,
    prefixo_peca varchar(10) not null default 'PC',
    separador_peca varchar(3) not null default '-',
    digitos_peca integer not null default 4
);

create table onboarding_unidade (
    unidade_id uuid primary key references unidade(id),
    etapas_concluidas integer not null default 0 check (etapas_concluidas between 0 and 255)
);

create table onboarding_usuario (
    unidade_id uuid not null references unidade(id),
    usuario_id uuid not null references usuario(id),
    apresentacao_vista boolean not null default false,
    conclusao_dispensada boolean not null default false,
    primary key (unidade_id, usuario_id)
);
alter table contratante add constraint uk_contratante_unidade_id unique (unidade_id, id);
alter table empresa add constraint uk_empresa_unidade_id unique (unidade_id, id);
alter table item_catalogo add constraint uk_item_catalogo_unidade_id unique (unidade_id, id);
alter table orcamento add constraint uk_orcamento_unidade_id unique (unidade_id, id);
alter table revisao_orcamento add constraint uk_revisao_orcamento_unidade_id unique (unidade_id, id);
alter table contrato add constraint uk_contrato_unidade_id unique (unidade_id, id);
alter table servico add constraint uk_servico_unidade_id unique (unidade_id, id);
alter table modelo_contrato add constraint uk_modelo_contrato_unidade_id unique (unidade_id, id);
alter table configuracao_unidade add constraint uk_configuracao_unidade_unidade_id unique (unidade_id, id);
alter table empresa add constraint fk_empresa_matriz_id_unidade foreign key (unidade_id, matriz_id) references empresa(unidade_id, id);
alter table forma_recebimento add constraint fk_forma_recebimento_configuracao_unidade_id_unidade foreign key (unidade_id, configuracao_unidade_id) references configuracao_unidade(unidade_id, id);
alter table revisao_orcamento add constraint fk_revisao_orcamento_orcamento_id_unidade foreign key (unidade_id, orcamento_id) references orcamento(unidade_id, id);
alter table revisao_orcamento add constraint fk_revisao_orcamento_contratante_id_unidade foreign key (unidade_id, contratante_id) references contratante(unidade_id, id);
alter table revisao_orcamento add constraint fk_revisao_orcamento_empresa_id_unidade foreign key (unidade_id, empresa_id) references empresa(unidade_id, id);
alter table item_revisao_orcamento add constraint fk_item_revisao_orcamento_revisao_orcamento_id_unidade foreign key (unidade_id, revisao_orcamento_id) references revisao_orcamento(unidade_id, id);
alter table item_revisao_orcamento add constraint fk_item_revisao_orcamento_item_catalogo_id_unidade foreign key (unidade_id, item_catalogo_id) references item_catalogo(unidade_id, id);
alter table contrato add constraint fk_contrato_contratante_id_unidade foreign key (unidade_id, contratante_id) references contratante(unidade_id, id);
alter table contrato add constraint fk_contrato_orcamento_id_unidade foreign key (unidade_id, orcamento_id) references orcamento(unidade_id, id);
alter table contrato add constraint fk_contrato_empresa_id_unidade foreign key (unidade_id, empresa_id) references empresa(unidade_id, id);
alter table contrato add constraint fk_contrato_modelo_contrato_id_unidade foreign key (unidade_id, modelo_contrato_id) references modelo_contrato(unidade_id, id);
alter table servico add constraint fk_servico_contratante_id_unidade foreign key (unidade_id, contratante_id) references contratante(unidade_id, id);
alter table servico add constraint fk_servico_orcamento_id_unidade foreign key (unidade_id, orcamento_id) references orcamento(unidade_id, id);
alter table revisao_orcamento add constraint uk_revisao_orcamento_pai_id unique (orcamento_id,id);
alter table evento_orcamento add constraint fk_evento_revisao_pai foreign key (orcamento_id,revisao_orcamento_id) references revisao_orcamento(orcamento_id,id);
create index ix_associacao_unidade_ativa on associacao(unidade_id, ativa);
create index ix_associacao_usuario on associacao(usuario_id);
create index ix_convite_unidade_estado on convite_unidade(unidade_id, revogada_em, aceita_em);
create index ix_recuperacao_usuario_data on recuperacao_senha(usuario_id, criado_em);
create index ix_contratante_unidade_cadastro_nome on contratante(unidade_id, ativo, nome_razao_social);
create index ix_contato_contratante on contato_contratante(contratante_id);
create index ix_empresa_unidade_cadastro_nome on empresa(unidade_id, ativo, razao_social);
create index ix_catalogo_unidade_cadastro_nome on item_catalogo(unidade_id, ativo, nome);
create index ix_orcamento_unidade_estado_data on orcamento(unidade_id, status, criado_em);
create index ix_orcamento_unidade_data on orcamento(unidade_id, criado_em, id);
create index ix_evento_orcamento_historico on evento_orcamento(orcamento_id, ocorrido_em, id);
create index ix_contrato_unidade_cadastro_estado_data on contrato(unidade_id, ativo, status, criado_em);
create index ix_servico_unidade_periodo on servico(unidade_id, data_programada, data_fim_programada);
create index ix_servico_unidade_estado_data on servico(unidade_id, status, criado_em);
create index ix_pagamento_unidade_ativo on forma_recebimento(unidade_id, ativa);
create index ix_modelo_unidade_estado on modelo_contrato(unidade_id, status);
create index ix_arquivo_unidade on arquivo(unidade_id);
