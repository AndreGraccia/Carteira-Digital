# ADR 0014: descrições criptografadas em repouso

Status: aceito e implementado. 25/09/2026.

## Contexto

A descrição de um lançamento diz com quem a pessoa se relaciona: "Pix recebido Marina Souza", "Concentrix S.A.", "Escola". Quem tivesse acesso ao banco, a um backup ou a um dump de desenvolvimento lia tudo em texto puro. O checklist de LGPD da auditoria de 24/09 e o ADR 0012 deixaram isso pendente.

## Decisão

1. `transaction.description` e `card_charge.description` são gravadas com AES-256-GCM (`security/FieldCipher`, conversor JPA `domain/EncryptedText`). Cada valor tem um IV aleatório de 96 bits e o formato `enc:v1:<base64(iv | cifra | tag)>`.
2. O IV aleatório faz o mesmo texto gerar cifras diferentes, então ninguém descobre pagadores repetidos comparando linhas. A tag do GCM faz um dado adulterado ou lido com outra chave dar erro, nunca texto trocado.
3. A chave vem de `data_encryption_key` (Base64, 32 bytes):
   - chave malformada impede a subida;
   - sem chave, o desenvolvimento grava em texto puro com aviso no log;
   - o `docker-compose.prod.yml` recusa subir sem a chave.
4. Linhas sem o prefixo foram gravadas antes e são lidas como estão. Não há migração de dados: a próxima sincronização regrava as descrições que mudarem.
5. A V30 muda as duas colunas para `TEXT`, porque a cifra é mais longa que o original. No PostgreSQL essa troca não reescreve a tabela.

Continuam abertos, de propósito:

- **Valor, data e tipo.** As somas, os filtros por período e os índices da V27 dependem deles.
- **Nome da carteira e dados do cadastro.** Nome e e-mail são usados no login e no envio de e-mail. A proteção deles é a exclusão da conta (ADR 0012).
- **Nenhuma consulta SQL filtra por descrição.** O agrupamento de recorrências acontece em Java, depois da leitura. Se um dia existir busca por texto no banco, ela vai precisar de outro desenho, como um índice de hash.

## Consequências

- Um dump do banco não revela mais com quem a pessoa troca dinheiro.
- Perder a chave torna as descrições ilegíveis, sem conserto. A chave deve ficar no cofre de segredos do deploy, fora do `.env` versionado.
- Não há rotação de chave. Quando for necessária, o prefixo ganha um identificador (`enc:v2:<id>:`) e a leitura aceita as duas chaves durante a troca. **Feito no ADR 0016.**
- O `WalliaApplicationTests` confere, contra o PostgreSQL, que o banco guarda `enc:v1:` e que a API devolve o texto.
