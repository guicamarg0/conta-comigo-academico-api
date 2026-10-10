package br.com.gestaoservicos.arquivo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class SupabaseStorageService {
    private final RestClient cliente;
    private final String chave,bucket,url;
    public SupabaseStorageService(@Value("${app.storage.supabaseUrl:${SUPABASE_URL:}}") String url,
        @Value("${app.storage.chaveServidor:${SUPABASE_SERVICE_ROLE_KEY:}}") String chave,@Value("${app.storage.bucket:${SUPABASE_STORAGE_BUCKET:documentos}}") String bucket) {
        this.url=url.replaceAll("/$","");this.chave=chave;this.bucket=bucket;
        var fabrica=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        cliente=RestClient.builder().requestFactory(fabrica).build();
    }
    public boolean configurado() {
        if(url.isBlank() && chave.isBlank())return false;
        if(url.isBlank() || chave.isBlank() || !bucket.matches("[A-Za-z0-9_-]+"))throw indisponivel();
        return true;
    }
    public void salvar(String caminho,byte[] conteudo,String tipo) {
        validar(caminho);
        try { cliente.post().uri(url+"/storage/v1/object/"+bucket+"/"+caminho)
            .header("Authorization","Bearer "+chave).header("apikey",chave).header("x-upsert","false")
            .contentType(MediaType.parseMediaType(tipo)).body(conteudo).retrieve().toBodilessEntity(); }
        catch(RestClientException e){throw indisponivel();}
    }
    public byte[] ler(String caminho) {
        validar(caminho);
        try { return cliente.get().uri(url+"/storage/v1/object/authenticated/"+bucket+"/"+caminho)
            .header("Authorization","Bearer "+chave).header("apikey",chave).retrieve().body(byte[].class); }
        catch(RestClientException e){throw indisponivel();}
    }
    private void validar(String caminho) {
        if(!configurado())throw indisponivel();
        if(!caminho.matches("[A-Za-z0-9/_-]+\\.(pdf|png|jpg)"))throw new IllegalArgumentException("Caminho inválido");
    }
    private ResponseStatusException indisponivel(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento de arquivos indisponível. Confira a configuração do Supabase.");}
}
