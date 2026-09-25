# ADR 0003: plano de saída, compras no cartão, metas, alertas e medição do Pergunte

Status: aceito, 23/09/2026. Continua o [ADR 0002](0002-limite-diario-e-perguntas.md) e fecha os cinco próximos passos da [estratégia](../estrategia-produto.md).

## Contexto

Depois do ADR 0002, a tela Futuro dizia quando o saldo ficava negativo, por quê e quanto custava. Ainda faltavam quatro coisas para a promessa "mostrar o que vai faltar antes de faltar":

1. As faturas futuras só conheciam compras já feitas. No sandbox, Posto Shell, iFood e Spotify somam R$ 630,90 em 76 dias (R$ 8,30 por dia) e não apareciam nas faturas seguintes.
2. O alerta dizia o problema, não a saída.
3. A pessoa precisava abrir o app para descobrir o buraco.
4. Guardar dinheiro só existia no simulador; nada ficava registrado.

E um risco operacional: o `/ask` não tinha limite de requisições, e ninguém sabia que perguntas as pessoas faziam.

## Decisão

**Compras do dia a dia no cartão viram uma segunda pergunta.** `DailySpending.estimateCard` faz a média das compras à vista no cartão dos últimos 90 dias (parceladas já estão nas faturas). Confirmada (`pattern_key = 'card-spending'`), `CashEventPlanner.cardSpending` põe cada dia futuro de compra na fatura em que ele cai, pelo calendário de fechamento do cartão, como evento `ESTIMATED` no vencimento.

**Plano de saída calculado.** `ExitPlan.suggest` testa, para a primeira janela negativa, até três ações, rodando a projeção de novo:

| Ação | Quando aparece |
| --- | --- |
| Combinar outra data para uma conta (aluguel, escola, contas, assinaturas) | Existe uma entrada depois da janela; escolhe a conta que mais melhora o ponto mais baixo |
| Gastar menos por dia | O gasto do dia a dia está confirmado e as contas sozinhas cabem |
| Quanto falta e até quando | Nenhuma ação acima resolve |

Fatura de cartão e parcela de empréstimo nunca são sugeridas para adiar: atrasar custa juros e multa, que é justamente o buraco. No sandbox, a saída calculada é pagar o aluguel de R$ 1.500 no dia 05/10, depois do salário, em vez do dia 01/10: nenhum dia fica negativo.

**Metas entram na projeção como compromisso.** Tabela `goal` (V16), até 10 por pessoa. Cada meta é uma saída `CONFIRMED` mensal no dia escolhido. Ela entra em "já tem destino" e no limite diário, que passa a responder "quanto posso gastar sem sacrificar a meta".

**Alertas por e-mail, só para quem ativar.** `FutureAlertService` roda todo dia às 8h (horário de Brasília; `future_alert_cron=-` desliga), recalcula a projeção de 30 dias de quem ativou e escreve uma vez por janela negativa nova (`future_alert.last_window_start`, V17). O e-mail leva datas, valores, custo máximo e o link, nada de dado bancário. A falha de um usuário não interrompe os outros.

**Pergunte à Wall.ia medido e limitado.** `AskUsage` limita 30 perguntas a cada 10 minutos por pessoa (429 `ASK_RATE_LIMIT`) e conta, por dia, só a intenção detectada (`ask_metric`, V15): sem texto e sem usuário. `GET /api/v1/future/ask/metrics` é só para administradores.

## Alternativas consideradas

| Alternativa | A favor | Contra |
| --- | --- | --- |
| Estimativa de cartão junto com a da conta, numa pergunta só | Menos um clique | A conta paga no dia; o cartão, no vencimento. Misturar erra a data do buraco |
| Sugerir "pague o mínimo da fatura" | Fecha qualquer buraco | É o caminho para o rotativo a 436% a.a.; contraria o propósito |
| Push no navegador em vez de e-mail | Mais imediato | Exige service worker e permissão; o e-mail já tem infraestrutura e roteiro de teste (Mailpit) |
| Guardar o texto das perguntas para melhorar o interpretador | Dados mais ricos | Texto livre pode conter dado pessoal; a intenção basta para decidir quando evoluir |
| Metas como recorrência detectada | Nada novo no banco | Meta é decisão do usuário, não padrão do histórico |

## Consequências

- A projeção fica completa para o que o Open Finance permite ver: contas fixas, dia a dia na conta, dia a dia no cartão, faturas, parcelas, empréstimos e metas. Cada item com origem e certeza.
- O alerta vira decisão com saída calculada, e chega sem o app aberto.
- Limites em memória (`AskUsage`, como os de login e da Gemini) valem para uma instância. Com mais de uma, migrar para Redis.
- A estimativa de cartão vai inteira para o cartão com mais compras à vista. Com vários cartões ativos, dividir por cartão.
- O job de alertas percorre usuários em série. Com milhares, paginar e paralelizar, ou mover para uma fila.

## Como reverter

- Cada peça é independente: apagar as decisões `card-spending`, as metas ou desligar o cron (`future_alert_cron=-`) desfaz o efeito sem mexer no resto.
- V15, V16 e V17 só criam tabelas novas; nada existente foi alterado.
- `ExitPlan` é uma classe sem Spring; remover o campo `exitPlan` da resposta não afeta a projeção.
