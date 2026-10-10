---
tags: [academico, dominio, regras-de-negocio]
---
# Regras de negócio

## Objetivo e limites

Autônomos e pequenas empresas preparam propostas comerciais, geram contratos e organizam serviços. O sistema registra valores propostos e aprovados; não apresenta esses valores como receita recebida. Assinatura digital, despesas e contabilidade não fazem parte deste fluxo.

## Usuários, unidades e permissões

- O usuário possui uma conta global. O vínculo com cada unidade determina seu perfil naquele contexto.
- O administrador geral cria unidades e administra o acesso global. A seleção da unidade não dispensa autorização na API.
- Perfis da unidade: Administrador (`ADMIN`), Gestor (`GESTOR`), Operador (`OPERADOR`) e Consulta (`CONSULTA`).
- Consulta acessa informações permitidas, sem alteração. Ações de gestão e administração têm permissões próprias; o backend é responsável pela validação final.
- No perfil acadêmico, cadastro público está fechado. O administrador inicial é criado pelas variáveis de ambiente; os demais acessos são administrados pelo sistema e pelos convites.
- Quando um usuário possui apenas uma unidade disponível, o login pode selecioná-la diretamente. Com várias unidades, o usuário escolhe o contexto.
- Senhas são armazenadas com hash. A opção Manter conectado usa sessão renovável por até 30 dias; a senha digitada não é persistida no navegador.
- Sair revoga a sessão de renovação. Recuperação de senha utiliza token temporário, de uso único; envio real requer configuração de e-mail.

## Cadastros da unidade

| Cadastro | Finalidade |
|---|---|
| Empresa prestadora | Emitente/contratada dos documentos, documento CPF ou CNPJ, endereço, contato e logo |
| Cliente/contratante | Destinatário do orçamento e contratante do contrato, pessoa física ou jurídica |
| Catálogo | Peças, serviços e mão de obra com descrição, unidade de medida e preço |
| Forma de pagamento | Opções e instruções reutilizadas nos documentos |
| Configuração da unidade | Identificação e parâmetros da unidade, códigos e condições padrão |
| Modelo de contrato | Conteúdo formatado reutilizável, variáveis e histórico de versões |

Ativo/inativo corresponde à disponibilidade do cadastro, não à situação comercial de um documento. A empresa prestadora pode representar um autônomo, usando CPF. CPF/CNPJ são apresentados com pontuação nos documentos. Busca de CEP auxilia o preenchimento, sem impedir digitação manual.

O catálogo e os itens do orçamento aceitam importação de CSV/Excel com o modelo de colunas: código, descrição, unidade de medida e valor unitário. Importações precisam respeitar validações e a unidade corrente.

## Orçamentos

- A numeração é sequencial por unidade, com prefixo configurado e código numérico; não incorpora o ano.
- O orçamento contém emitente, cliente, itens, quantidades, preços, ajustes, validade e condições comerciais.
- Valores finais são calculados pelo backend; valores enviados pelo navegador não são a fonte de verdade.
- O assistente finaliza com **Salvar**. A API mantém os estados internos `RASCUNHO`, `EMITIDO`, `APROVADO`, `RECUSADO` e `CANCELADO`; a emissão depende das permissões do usuário.
- Somente orçamento emitido pode receber decisão de aprovação ou recusa. Cancelamento depende da situação de origem.
- Alterações/revisões preservam o histórico. Emissão armazena snapshots das partes e dos itens, além do PDF daquela revisão.
- Rascunhos podem gerar uma prévia PDF sem emissão. O PDF emitido e armazenado preserva o conteúdo original de sua revisão.
- Pode escolher incluir campos de assinatura e forma de pagamento no PDF.
- Condições comerciais refletem observações, validade e instruções de pagamento.
- Nome do arquivo: código do documento e nome do cliente, sanitizados para download.
- PDFs novos têm estrutura neutra, logo do emitente e timbrado discreto nos cantos superior esquerdo e inferior direito de cada folha.
- Assinaturas aparecem apenas na última página e devem acompanhar o encerramento com conteúdo, evitando uma página exclusivamente de assinaturas.

## Contratos

| Aspecto | Regra |
|---|---|
| Origem | Um contrato por orçamento aprovado; não há contrato independente |
| Situação inicial | Aguardando assinatura |
| Outras situações | Vigente e Encerrado |
| Cadastro | Ativo ou Inativo, separado da situação |
| Assinatura | Realizada fora do sistema; mudança para Vigente é manual |
| Encerramento | Manual, a partir de Vigente; bloqueia edição/restauração |
| Inativação | Pode ocorrer em qualquer situação; não apaga o contrato nem reabre um encerrado |
| Edição | Permitida em Aguardando assinatura ou Vigente, inclusive quando Inativo |
| Salvamento | Mudança de conteúdo cria versão com autoria/data; sem mudança não cria nova versão de conteúdo |
| Histórico | Versões do conteúdo e atividades de situação/cadastro, paginadas |
| Restauração | Recupera o texto escolhido, cria versão nova e conserva vínculos, cadastro e situação atuais |
| Permissões | Administrador, Gestor e Operador podem executar ações de contrato permitidas; Consulta não altera |

Encerrar, inativar e restaurar exigem confirmação. Não há exclusão de contratos nem reabertura automática de encerrados. Consulta e PDF permanecem disponíveis após encerramento.

O modelo permite texto formatado, parágrafos, tabelas e variáveis para os dados comerciais. O PDF preserva a formatação suportada pelo renderizador e inclui campos fixos de assinatura das partes na última página. O editor possui histórico e prévia; seu salvamento é apresentado como uma única ação Salvar.

## Serviços e agenda

- Serviço pode ser independente ou vinculado a orçamento aprovado.
- Um orçamento aprovado pode originar vários serviços. Serviço não exige contrato nem vínculo direto com ele.
- Categoria e equipe não fazem parte do cadastro atual.
- Programação contém data inicial e final, sem exigir horário. O período é inclusivo e ocupa todos os dias informados.
- Vários serviços podem ocupar o mesmo dia; a agenda organiza a visualização, sem bloquear sobreposição por exclusividade de horário.
- Situações técnicas disponíveis: Rascunho, Agendado, Em andamento, Aguardando, Concluído e Cancelado. As ações dependem da situação e das permissões.
- Convite de calendário é um arquivo ICS de dia inteiro. Sua data final exclusiva é ajustada para representar todo o período na agenda externa.
- A cor identifica o serviço e é mantida durante seu período; cores também aparecem nas linhas do modo Lista.

## Dashboard, preparação e demonstração

O dashboard resume indicadores operacionais e valores de orçamento, sem tratá-los como pagamentos recebidos. A preparação orienta o cadastro da unidade até orçamento, contrato e serviço; o progresso permite abrir a etapa correspondente.

A demonstração usa exemplos fictícios no navegador, em armazenamento separado da sessão real. Restaurar a demo não deve apagar dados do banco nem a autenticação da API.

Veja [[Arquitetura]] para a implementação dessas regras e as fronteiras de autorização.
