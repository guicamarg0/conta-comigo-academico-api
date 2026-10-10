package br.com.gestaoservicos.empresa.dto;
import br.com.gestaoservicos.empresa.model.TipoEmpresa; import java.util.UUID;
public record EmpresaResponseDTO(UUID id, TipoEmpresa tipo, UUID matrizId, String matrizNome, String razaoSocial, String nomeFantasia, String cnpj, String inscricaoEstadual, String telefone, String email, String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String estado, boolean ativo, String logoUrl) {}
