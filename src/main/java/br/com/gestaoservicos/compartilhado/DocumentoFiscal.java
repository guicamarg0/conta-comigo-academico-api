package br.com.gestaoservicos.compartilhado;

public final class DocumentoFiscal {
    private DocumentoFiscal() {}
    public static String formatarNoTexto(String conteudo, String... documentos) {
        if (conteudo == null || conteudo.isBlank()) return conteudo;
        var html = org.jsoup.Jsoup.parseBodyFragment(conteudo);
        boolean[] alterado = {false};
        org.jsoup.select.NodeTraversor.traverse(new org.jsoup.select.NodeVisitor() {
            public void head(org.jsoup.nodes.Node node, int profundidade) {
                if (!(node instanceof org.jsoup.nodes.TextNode texto)) return;
                String valor = texto.getWholeText();
                for (String documento : documentos) {
                    if (documento == null || !documento.matches("\\d{11}|\\d{14}")) continue;
                    valor = valor.replaceAll("(?<!\\d)" + documento + "(?!\\d)", formatar(documento));
                }
                if (!valor.equals(texto.getWholeText())) { texto.text(valor); alterado[0] = true; }
            }
            public void tail(org.jsoup.nodes.Node node, int profundidade) {}
        }, html.body());
        if (!alterado[0]) return conteudo;
        return conteudo.matches("(?s).*<[^>]+>.*") ? html.body().html() : html.body().wholeText();
    }
    public static String formatar(String valor) {
        if (valor==null) return "";
        String d=valor.replaceAll("\\D", "");
        if (d.length()==11) return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        if (d.length()==14) return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        return valor;
    }
    public static boolean cpfValido(String valor) {
        if (valor==null || !valor.matches("\\d{11}") || valor.chars().distinct().count()==1) return false;
        for (int posicao=9; posicao<=10; posicao++) {
            int soma=0;
            for (int i=0; i<posicao; i++) soma+=(valor.charAt(i)-'0')*(posicao+1-i);
            int digito=(soma*10)%11; if (digito==10) digito=0;
            if (valor.charAt(posicao)-'0'!=digito) return false;
        }
        return true;
    }
}
