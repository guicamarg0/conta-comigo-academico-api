package br.com.gestaoservicos.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

@Configuration
public class ConfiguracaoArquivos {
    @Bean
    @ConditionalOnMissingBean(MultipartConfigElement.class)
    MultipartConfigElement multipartConfigElement(Environment ambiente) {
        var fabrica=new MultipartConfigFactory();
        fabrica.setMaxFileSize(DataSize.parse(ambiente.getProperty("spring.servlet.multipart.max-file-size","5MB")));
        fabrica.setMaxRequestSize(DataSize.parse(ambiente.getProperty("spring.servlet.multipart.max-request-size","6MB")));
        return fabrica.createMultipartConfig();
    }
}
