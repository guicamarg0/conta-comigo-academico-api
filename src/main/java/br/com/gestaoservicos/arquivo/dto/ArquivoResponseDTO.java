package br.com.gestaoservicos.arquivo.dto;
import java.util.UUID;
public record ArquivoResponseDTO(UUID id,String nome,String tipo,String caminhoDownload) {}
