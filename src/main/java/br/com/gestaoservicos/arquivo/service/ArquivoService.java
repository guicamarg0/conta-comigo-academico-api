package br.com.gestaoservicos.arquivo.service;
import br.com.gestaoservicos.arquivo.model.Arquivo;
import br.com.gestaoservicos.arquivo.repository.ArquivoRepository;
import br.com.gestaoservicos.arquivo.dto.ArquivoResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@Service
public class ArquivoService {
    private final ArquivoRepository arquivos;private final SupabaseStorageService storage;
    public ArquivoService(ArquivoRepository arquivos,SupabaseStorageService storage){this.arquivos=arquivos;this.storage=storage;}
    @Transactional
    public ArquivoResponseDTO enviar(UUID unidade,MultipartFile arquivo) {
        if(arquivo.isEmpty()||arquivo.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione PDF, PNG ou JPG de até 5 MB.");
        try {
            byte[] bytes=arquivo.getBytes();String tipo,extensao;
            if(bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
                try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){if(pdf.getNumberOfPages()==0)throw new java.io.IOException();}
                tipo="application/pdf";extensao="pdf";
            } else {
                var imagem=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if(imagem==null)throw new java.io.IOException();
                if(bytes[0]==(byte)0x89 && bytes[1]==0x50){tipo="image/png";extensao="png";}
                else if(bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8){tipo="image/jpeg";extensao="jpg";}
                else throw new java.io.IOException();
            }
            String nome=arquivo.getOriginalFilename()==null?"comprovante."+extensao:arquivo.getOriginalFilename().replaceAll("[\\\\/\\r\\n]","_");
            if(nome.length()>200)nome=nome.substring(nome.length()-200);
            var registro=new Arquivo(unidade,nome,tipo,extensao);
            registro.armazenarNoBanco(bytes);arquivos.save(registro);
            return new ArquivoResponseDTO(registro.getId(),nome,tipo,"/arquivos/"+registro.getId());
        } catch(java.io.IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Arquivo inválido. Use PDF, PNG ou JPG.");}
    }
    @Transactional(readOnly=true)
    public Arquivo consultar(UUID unidade,UUID id) {return arquivos.findByIdAndUnidadeId(id,unidade).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Arquivo não encontrado"));}
    public byte[] conteudo(Arquivo arquivo){
        byte[] bytes=arquivo.getConteudo();
        return bytes!=null?bytes:storage.ler(arquivo.getCaminho());
    }
}
