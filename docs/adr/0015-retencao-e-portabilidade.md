# ADR 0015: retenção de dados e exportação para o titular

Status: aceito e implementado. 25/09/2026.

## Contexto

O ADR 0012 deu à pessoa a exclusão da conta, mas a LGPD pede mais duas coisas que faltavam:

- **Acesso e portabilidade (art. 18, II e V).** Não havia como a pessoa receber os próprios dados.
- **Término do tratamento (art. 15 e 16).** Nada apagava dados que já cumpriram a finalidade. Códigos de e-mail, snapshots da previsão e a auditoria de admin cresciam para sempre.

A auditoria do ChatGPT de 25/09 e o backlog (B20, B21, B22) apontaram também que a tela de Privacidade dizia "apaga o cadastro", quando a exclusão pseudonimiza e mantém o histórico.

## Decisão

### Exportação

`GET /api/v1/account/export` devolve um JSON com tudo o que existe sobre quem chama: cadastro, papéis, carteiras, lançamentos, cartões, compras, empréstimos, consentimentos do Open Finance, metas, envelopes, respostas, alertas, snapshots da previsão, aparelhos com aviso e ações de admin.

- As colunas são listadas uma a uma (`AccountExportService`). Coluna nova só entra quando alguém a coloca na lista, então um segredo criado depois nunca sai por padrão.
- Ficam de fora: hash da senha, hash do código de e-mail, corte de sessão (`tokens_valid_after`) e as chaves do aviso no navegador (`p256dh`, `auth`).
- As descrições saem decifradas (ADR 0014). Datas saem em texto ISO.
- Resposta com `Content-Disposition: attachment` e `Cache-Control: no-store`.
- Sem limitador: o limitador existente conta tentativas de login, e cinco downloads bloqueariam o login da pessoa.
- A tela Privacidade ganhou "Baixar meus dados". O arquivo é montado no navegador a partir da resposta, então o token nunca vira link.

### Retenção

- **Códigos de confirmação de e-mail.** 30 dias. O código vale minutos; 30 dias cobrem reenvio e dúvidas de suporte. Sai por: `RetentionService`, diário às 3h30.
- **Snapshots da previsão.** 400 dias. O placar de precisão lê até 365 dias, mais 30 de antecedência. Sai por: `RetentionService`.
- **Auditoria de ações de admin.** 1 ano. Revisão de incidentes. Sai por: `RetentionService`.
- **Dados do Open Finance.** Enquanto o consentimento vale. Finalidade do consentimento. Sai por: Apagados ao revogar, vencer ou ser revogado no banco (ADR 0008, 0009).
- **Cadastro, carteiras, lançamentos, metas, envelopes, respostas, alertas.** Enquanto a conta existe. Prestar o serviço. Sai por: Exclusão da conta (ADR 0012).
- **Linha do usuário, carteiras com movimentação e seus lançamentos, depois da exclusão.** Sem prazo, pseudonimizados. Uma transferência aparece no extrato de outra pessoa (art. 16). Sai por: Nome, usuário e e-mail trocados; senha aleatória.
- **Logs dos containers.** 5 arquivos de 100 MB por serviço. Diagnóstico. Sai por: Rotação do Docker.

O job usa o `JobLock` (uma instância executa) e o cron `retention_cron` (`-` desliga).

## Consequências

- A pessoa consegue ver e levar tudo o que a Wall.ia guarda, sem pedir ao suporte.
- A tela de exclusão agora descreve o que o código faz: pseudonimização com retenção do histórico, e não apagamento total.
- **Em aberto, fora deste ADR:**
  - **Marco Civil, art. 15.** Um provedor de aplicação constituído como empresa precisa guardar registros de acesso (IP, data e hora) por 6 meses. Hoje não há registro de acesso próprio, e os logs do Docker giram por tamanho, não por prazo. Decidir antes da produção, junto com o jurídico.
  - **Backups.** Resolvido na Sprint 13: `scripts/backup.sh` guarda 14 dias (`BACKUP_KEEP_DAYS`). A exclusão e a retenção se completam quando o último backup com o dado expira, em até 14 dias ([`operacao.md`](../operacao.md), seção 2).
