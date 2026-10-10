package br.com.gestaoservicos;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import br.com.gestaoservicos.compartilhado.NomeDocumento;
class NomeDocumentoTest {
 @Test void compactaCodigoESanitizaCliente(){
  assertThat(NomeDocumento.pdf("ORC-2026-000052","Metal Filme LTDA")).isEqualTo("ORC-52_Metal-Filme-LTDA.pdf");
  assertThat(NomeDocumento.pdf("CT-A1B2C3D4","João / Silva")).isEqualTo("CT-A1B2C3D4_Joao-Silva.pdf");
  assertThat(NomeDocumento.pdf("ORC-1",null)).isEqualTo("ORC-1_Cliente.pdf");
 }
}
