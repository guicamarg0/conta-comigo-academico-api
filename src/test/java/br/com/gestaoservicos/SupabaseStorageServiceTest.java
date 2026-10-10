package br.com.gestaoservicos;

import br.com.gestaoservicos.arquivo.service.SupabaseStorageService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class SupabaseStorageServiceTest {
    @Test void enviaELerArquivoPrivadoSemSobrescrever() throws Exception {
        var servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var recebido=new AtomicReference<byte[]>();
        var autorizacao=new AtomicReference<String>();
        var sobrescrever=new AtomicReference<String>();
        byte[] pdf="%PDF-teste".getBytes(StandardCharsets.UTF_8);
        servidor.createContext("/storage/v1/object/documentos/unidade/teste.pdf", troca->{
            recebido.set(troca.getRequestBody().readAllBytes());
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            sobrescrever.set(troca.getRequestHeaders().getFirst("x-upsert"));
            troca.sendResponseHeaders(200,0);troca.close();
        });
        servidor.createContext("/storage/v1/object/authenticated/documentos/unidade/teste.pdf",troca->{
            troca.sendResponseHeaders(200,pdf.length);troca.getResponseBody().write(pdf);troca.close();
        });
        servidor.start();
        try {
            var storage=new SupabaseStorageService("http://127.0.0.1:"+servidor.getAddress().getPort(),"chave-sintetica","documentos");
            storage.salvar("unidade/teste.pdf",pdf,"application/pdf");
            assertThat(recebido.get()).isEqualTo(pdf);
            assertThat(autorizacao.get()).isEqualTo("Bearer chave-sintetica");
            assertThat(sobrescrever.get()).isEqualTo("false");
            assertThat(storage.ler("unidade/teste.pdf")).isEqualTo(pdf);
            assertThatThrownBy(()->storage.ler("../teste.pdf")).isInstanceOf(IllegalArgumentException.class);
        } finally {servidor.stop(0);}
    }
    @Test void configuracaoAusenteOuParcialTemRespostaControlada() {
        assertThat(new SupabaseStorageService("","","documentos").configurado()).isFalse();
        assertThatThrownBy(()->new SupabaseStorageService("https://exemplo.invalid","","documentos").configurado())
            .isInstanceOf(ResponseStatusException.class);
    }
}
