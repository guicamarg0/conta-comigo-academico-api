# Conta Comigo — API acadêmica

Cópia independente para estudo de desenvolvimento de software: orçamentos como centro do fluxo comercial, contratos e organização dos serviços. Backend Java 21 / Spring Boot 4.1, PostgreSQL, JPA e Flyway. A cópia começa com histórico Git próprio e não contém bancos, credenciais ou configurações do ambiente original.

## Executar o sistema completo com Docker

Pré-requisitos: Git e Docker Engine com Docker Compose v2 (ou Docker Desktop em execução). Não é necessário instalar Java, Maven, Node ou PostgreSQL no computador.

Clone **os dois repositórios lado a lado**, mantendo estes nomes de pasta:

```sh
git clone https://github.com/guicamarg0/conta-comigo-academico-api.git
git clone https://github.com/guicamarg0/conta-comigo-academico-web.git
cd conta-comigo-academico-api
```

Copie o arquivo de configuração:

```sh
# Linux / macOS / Git Bash
cp .env.example .env
```

```powershell
# PowerShell no Windows
Copy-Item .env.example .env
```

```sh
docker compose up --build -d --wait --wait-timeout 300
```

O Compose cria um banco PostgreSQL vazio, aplica as migrations, cria o administrador local e sobe API e frontend. O primeiro build depende da internet para baixar imagens e dependências e pode demorar alguns minutos.

| Recurso | Endereço padrão |
|---|---|
| Interface | http://localhost:3000 |
| API | http://localhost:8080/api/v1 |
| Saúde da API | http://localhost:8080/actuator/health |
| Swagger | http://localhost:8080/swagger-ui.html |

As portas são publicadas somente no endereço de loopback. O banco fica acessível apenas na rede interna do Compose. Há um volume próprio para os dados acadêmicos.

## Primeiro acesso

Com os exemplos locais do `.env.example`:

- E-mail: `admin@academico.test`
- Senha: `Academico-local-2026!`

Esses valores são **públicos e fictícios**, destinados exclusivamente à demonstração em localhost. Para outro ambiente, gere credenciais e segredo JWT próprios. Nunca versione o `.env`.

1. Entre com o administrador geral e crie uma unidade em Administração geral.
2. Acesse a unidade e siga a preparação: empresa prestadora, formas de pagamento, configuração da unidade, clientes, peças/serviços do catálogo e modelos de contrato.
3. Crie e salve um orçamento com seus itens e confira o PDF.
4. Registre sua aprovação para gerar um contrato ou vincular um serviço.
5. Programe um serviço com datas de início/fim e confira sua presença na agenda.

Não há dados de clientes pré-carregados. A interface também oferece uma demonstração com exemplos fictícios, separada do banco da API. A senha do administrador é utilizada apenas na criação inicial; alterar o `.env` depois não troca a senha de um usuário existente.

## Comandos de operação

```sh
docker compose ps
docker compose logs -f api
docker compose stop
docker compose start
docker compose down
```

`down` remove os containers e a rede, preservando os dados do volume. Para reiniciar **apagando todos os dados acadêmicos locais**:

```sh
docker compose down --volumes
docker compose up --build -d --wait --wait-timeout 300
```

Se as portas estiverem ocupadas, altere `WEB_PORT`, `API_PORT` e `FRONTEND_URL` no `.env`. Por exemplo: `WEB_PORT=3100`, `API_PORT=8181`, `FRONTEND_URL=http://localhost:3100`. Inicie novamente o Compose. Não abra `index.html` diretamente pelo sistema de arquivos.

## Testes e desenvolvimento da API

Com Java 21 e Maven 3.9 instalados:

```sh
mvn test
mvn package
```

Os testes usam H2 para integrações da aplicação. Os testes com Testcontainers exigem Docker disponível e podem ser ignorados quando ele não existe. H2 não substitui a validação das migrations no PostgreSQL.

Para desenvolvimento no host, forneça `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_GERAL_EMAIL` e `ADMIN_GERAL_SENHA` apontando para um PostgreSQL local próprio, e execute `mvn spring-boot:run -Dspring-boot.run.profiles=academico`. O banco do Compose não publica uma porta para o host por padrão.

## Documentação para a disciplina

Abra a pasta [`cofre`](cofre/Inicio.md) como cofre no Obsidian. Ela contém apenas as regras de negócio e a arquitetura desta versão, sem roteiros do deploy original, conversas de planejamento ou histórico operacional.

- [Regras de negócio](cofre/Regras%20de%20negocio.md)
- [Arquitetura](cofre/Arquitetura.md)
- [Frontend acadêmico](https://github.com/guicamarg0/conta-comigo-academico-web)

## Integrações opcionais e limites

- Recuperação de senha por e-mail exige `RESEND_API_KEY` e `EMAIL_REMETENTE` próprios. Sem eles, a aplicação informa indisponibilidade de envio; login e cadastros continuam disponíveis.
- Busca de CEP usa ViaCEP e depende da internet; o endereço pode ser preenchido manualmente.
- Logos, arquivos e PDFs novos usam o banco local. Não é necessário configurar armazenamento externo.
- Assinatura ocorre fora do sistema; os campos nos PDFs são visuais.
- Despesas e relatórios financeiros não fazem parte desta versão.

Esta cópia não sincroniza automaticamente com os repositórios originais e não aciona seus deploys.
