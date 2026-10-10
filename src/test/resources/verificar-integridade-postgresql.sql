-- Executar após V1 em schema isolado, dentro de transação finalizada com ROLLBACK.
DO $$
DECLARE
    primeira uuid := gen_random_uuid();
    segunda uuid := gen_random_uuid();
    cliente uuid := gen_random_uuid();
    id_servico_teste uuid := gen_random_uuid();
BEGIN
    INSERT INTO unidade(id,nome,nome_normalizado,criada_em) VALUES
        (primeira,'Unidade fictícia A','unidade ficticia a',current_timestamp),
        (segunda,'Unidade fictícia B','unidade ficticia b',current_timestamp);
    INSERT INTO contratante(id,unidade_id,tipo,nome_razao_social,documento,ativo,criado_em,atualizado_em)
        VALUES (cliente,primeira,'PF','Cliente fictício','00000000000',true,current_timestamp,current_timestamp);
    INSERT INTO servico(id,unidade_id,contratante_id,codigo,titulo,status,criado_em,atualizado_em)
        VALUES (id_servico_teste,primeira,cliente,'SV-1','Teste de integridade','RASCUNHO',current_timestamp,current_timestamp);
    BEGIN
        INSERT INTO servico(id,unidade_id,contratante_id,codigo,titulo,status,criado_em,atualizado_em)
            VALUES (gen_random_uuid(),segunda,cliente,'SV-1','Vínculo inválido','RASCUNHO',current_timestamp,current_timestamp);
        RAISE EXCEPTION 'O banco aceitou um cliente de outra unidade';
    EXCEPTION WHEN foreign_key_violation THEN
        NULL;
    END;
    BEGIN
        UPDATE servico SET unidade_id=segunda WHERE id=id_servico_teste;
        RAISE EXCEPTION 'O banco aceitou a troca de unidade incompatível';
    EXCEPTION WHEN foreign_key_violation THEN
        NULL;
    END;
    IF (SELECT unidade_id FROM servico WHERE id=id_servico_teste) <> primeira THEN
        RAISE EXCEPTION 'O vínculo original não foi preservado';
    END IF;
END $$;
