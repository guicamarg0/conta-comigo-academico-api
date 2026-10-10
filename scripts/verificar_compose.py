"""Smoke exclusivo da instalação acadêmica local, com credenciais fictícias."""
import json
import os
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.environ.get("FRONTEND_URL", "http://localhost:3000").rstrip("/")


def chamada(caminho, corpo=None, cabecalhos=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    headers = {"Content-Type": "application/json", **(cabecalhos or {})}
    with urlopen(Request(BASE + caminho, data=dados, headers=headers), timeout=30) as resposta:
        raw = resposta.read()
        return resposta.status, resposta.headers, json.loads(raw) if raw else None


with urlopen(BASE + "/login", timeout=30) as pagina:
    assert pagina.status == 200 and b'id="root"' in pagina.read(), "SPA indisponível"

with urlopen(BASE + "/health", timeout=30) as resposta:
    assert resposta.read().strip() == b"ok", "Health do frontend indisponível"

status, headers, login = chamada("/api/v1/autenticacao/login", {
    "email": "admin@academico.test", "senha": "Academico-local-2026!", "manterConectado": True,
})
assert status == 200 and login["tokenAcesso"], "Login não concluído"
cookie = SimpleCookie(headers["Set-Cookie"])
assert cookie["conta_comigo_renovacao"]["httponly"], "Cookie sem HttpOnly"
assert cookie["conta_comigo_renovacao"]["secure"], "Cookie sem Secure"
assert cookie["conta_comigo_renovacao"]["max-age"] == "2592000", "Persistência diferente de 30 dias"
autorizacao = {"Authorization": "Bearer " + login["tokenAcesso"]}
status, _, unidade = chamada("/api/v1/unidades", {"nome": "Unidade Acadêmica Fictícia"}, autorizacao)
assert status == 201 and unidade["id"], "Unidade não criada"
autorizacao["X-Unidade-Id"] = unidade["id"]
status, _, protegida = chamada("/api/v1/unidades/atual/protegida", cabecalhos=autorizacao)
assert status == 200 and protegida["unidadeId"] == unidade["id"], "Unidade não autorizada"

sessao = {"Origin": BASE, "Cookie": "conta_comigo_renovacao=" + cookie["conta_comigo_renovacao"].value}
status, headers, renovado = chamada("/api/v1/autenticacao/renovar", {}, sessao)
assert status == 200 and renovado["tokenAcesso"], "Sessão não renovada pelo proxy"
novo = SimpleCookie(headers["Set-Cookie"])["conta_comigo_renovacao"].value
assert novo != cookie["conta_comigo_renovacao"].value, "Token de renovação não rotacionado"
sessao["Cookie"] = "conta_comigo_renovacao=" + novo
status, _, _ = chamada("/api/v1/autenticacao/sair", {}, sessao)
assert status == 204, "Logout não concluído"
try:
    chamada("/api/v1/autenticacao/renovar", {}, sessao)
    raise AssertionError("Sessão continuou válida depois de sair")
except HTTPError as erro:
    assert erro.code == 401, "Status inesperado após logout"

print("SPA, proxy, login, bootstrap, unidade, cookie de 30 dias, renovação e logout aprovados.")
