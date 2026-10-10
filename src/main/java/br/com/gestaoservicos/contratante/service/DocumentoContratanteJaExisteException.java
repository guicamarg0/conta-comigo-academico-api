package br.com.gestaoservicos.contratante.service;
public class DocumentoContratanteJaExisteException extends RuntimeException { public DocumentoContratanteJaExisteException() { super("Já existe um contratante com este documento na unidade"); } }
