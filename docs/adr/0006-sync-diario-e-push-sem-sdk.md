# ADR 0006: sync diário, avisos por Web Push sem SDK e painel da métrica principal

Status: aceito e implementado. 24/09/2026.

## Contexto

Depois do ADR 0005 ficaram três lacunas:

1. O placar de precisão e os alertas das 8h dependiam de a pessoa sincronizar à mão. Sem sync, o alerta lia saldo de dias atrás.
2. O alerta só saía por e-mail, que exige e-mail verificado e quase ninguém lê a tempo.
3. A métrica principal era medida por usuário, mas o time não tinha como ver o agregado, nem quantas pessoas queriam o Pix de verdade.

## Decisão

**Sync diário (`OpenFinanceDailySync`).** Às 7h30 (Brasília), cada usuário com conexão autorizada passa pelo mesmo `OpenFinanceService.sync` do botão, que confere consentimento e e-mail verificado; vencido é pulado, nunca renovado. Depois do commit do sync, grava o placar. Falha de um usuário não para os outros. `app.open-finance.daily-sync-cron` com `-` desliga. Admin roda na hora por `POST /admin/open-finance/daily-sync`.

**Web Push próprio.** `WebPush` implementa RFC 8291 (aes128gcm) e RFC 8292 (VAPID, ES256) com a criptografia do JDK. Sem dependência nova: a rede da máquina não baixa bibliotecas, e o código cabe em uma classe. O teste confere o corpo cifrado byte a byte com o exemplo da RFC 8291.

- Inscrições em `push_subscription` (V22), até 5 navegadores por pessoa; um navegador pertence a uma conta só.
- O servidor só envia para os serviços de push dos navegadores (FCM, Mozilla, Apple, Windows), em https na porta 443. A URL vem do cliente; sem essa lista, a inscrição seria SSRF.
- O texto é fixo e mostra só a data, porque aparece na tela de bloqueio.
- 404 ou 410 do serviço apaga a inscrição.
- O alerta sai por e-mail (se verificado) e por push (se houver navegador). Ligar o alerta exige ao menos um dos dois. A janela só é marcada como avisada se algum canal entregou.
- O `serviceWorker.unregister()` do template do CRA saiu do `index.js`: ele desinscreveria o push a cada carga. O `sw.js` não guarda cache nem intercepta requisição.
- Sem chaves VAPID configuradas, o push fica desligado e a tela não oferece a opção.

**Painel (`GET /admin/metrics`).** Negativos evitados por usuário ativo por mês, negativos sem aviso, erro médio, perguntas sem resposta e cliques em "Preparar Pix" (contados na mesma tabela das intenções do Pergunte, sem usuário). Tela só para admin, auditada como as outras rotas de admin.

## Descartado

- Biblioteca `web-push` para Java: nova dependência, sem rede para baixar, e faria o mesmo que 150 linhas testadas contra a RFC.
- Reinscrição pelo evento `pushsubscriptionchange` no service worker: ele não tem o token de sessão. A tela Futuro reenvia a inscrição a cada visita.
- Sync em paralelo: desnecessário na escala atual; marcado no código como teto conhecido.

## Verificado ao vivo

Chrome se inscreveu pelo botão da tela, o job de alertas cifrou e enviou ao FCM, e o service worker exibiu "Wall.ia: seu saldo pode ficar negativo | A partir de 03/10. Toque para ver por quê e como sair.".
