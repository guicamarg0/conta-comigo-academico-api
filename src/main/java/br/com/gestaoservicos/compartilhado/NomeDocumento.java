package br.com.gestaoservicos.compartilhado;

public final class NomeDocumento {
    private NomeDocumento() {}
    public static String pdf(String numero,String cliente) {
        String codigo=numero.replaceFirst("^(ORC)-\\d{4}-0*(\\d+)$", "$1-$2");
        return seguro(codigo,30)+"_"+seguro(cliente==null?"Cliente":cliente,70)+".pdf";
    }
    private static String seguro(String valor,int limite) {
        String s=java.text.Normalizer.normalize(valor,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^A-Za-z0-9_-]+","-").replaceAll("^-+|-+$","");
        if(s.isBlank())s="Cliente";
        return s.substring(0,Math.min(limite,s.length()));
    }
}
