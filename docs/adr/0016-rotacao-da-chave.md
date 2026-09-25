# ADR 0016: rotação da chave de criptografia

Status: aceito e implementado. 25/09/2026.

## Contexto

O ADR 0014 cifrou as descrições com uma chave só, sem identificação: o formato `enc:v1:` não dizia com qual chave o valor foi gravado. Trocar a chave tornaria ilegível tudo o que já existia. E não havia como responder a um vazamento da chave, nem aposentar uma chave antiga.

## Decisão

1. Valores novos saem como `enc:v2:<id>:<base64(iv | cifra | tag)>`. O id são os 8 primeiros dígitos hexadecimais do SHA-256 da chave: identifica sem revelar, e ninguém precisa configurar id nenhum.
2. `data_encryption_key` é a chave que grava. `data_encryption_previous_keys` (separadas por vírgula) só leem.
3. Leitura:
   - `v2` usa a chave do id; id desconhecido é erro.
   - `v1` tenta a chave atual e depois as anteriores. A tag do GCM faz a chave errada falhar, então a tentativa nunca devolve texto trocado.
   - Sem prefixo é texto antigo e é lido como está.
4. `EncryptionRotationService`, às 3h45, regrava em lotes de 500 tudo o que não está na chave atual: `v1`, chave anterior e texto puro. Um valor que nenhuma chave abre é contado e deixado como está, porque pode ser a única cópia.
5. O procedimento de troca está em [`operacao.md`](../operacao.md), seção 3.

## Consequências

- A chave pode ser trocada sem parar o serviço e sem migração manual.
- As linhas gravadas antes de existir chave, que o ADR 0014 deixava em texto puro, passam a ser cifradas pelo mesmo job.
- Uma chave anterior só pode sair quando o job não tiver mais nada a regravar **e** o último backup feito com ela tiver expirado.
- O job lê as tabelas inteiras por faixa de id uma vez por dia. Quando não há nada a fazer, é uma consulta por tabela. Se as tabelas crescerem a ponto de pesar, dá para trocar o `NOT LIKE` por uma coluna com o id da chave e um índice.
