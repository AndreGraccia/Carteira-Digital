# ADR 0005: envelopes no lugar de Movimentar, placar de precisão e Pix agendado só simulado

Status: aceito e implementado. 24/09/2026.

## Contexto

Três pontos ficaram abertos no fim da rodada anterior:

1. **A tela Movimentar não tinha uso.** Carteira local nasce com zero, não há depósito e conta do Open Finance é só leitura. Transferir e retirar existiam só no código.
2. **Pagamento via Pix.** É o que leva o produto de "ver o futuro" para "agir sobre ele", mas exige credenciamento como iniciador de pagamento.
3. **A métrica estrela-guia não era medida.** A estratégia define "dias no negativo evitados por usuário ativo por mês", e nada no sistema comparava previsão com realidade.

O dono decidiu: Movimentar vira Envelopes; Pix só simulado nesta rodada.

## Decisão

### Envelopes (V20)

Um envelope é parte do saldo de hoje separada para algo que a projeção não conhece (viagem, IPVA, reserva). Nenhum dinheiro se move.

- O total dos envelopes vira **piso**: `SpendingLimit.compute` recebe `saldo - envelopes`. O limite diário passa a responder "quanto posso gastar sem mexer nos envelopes".
- A projeção, as janelas negativas e o plano de saída continuam no saldo real, porque o banco não conhece envelopes e o cheque especial começa no zero de verdade.
- "Margem livre" desconta os envelopes.
- No Pergunte, uma compra que deixaria o menor saldo abaixo de uma semana de gasto mais os envelopes sai como "apertado", nunca "tranquilo".
- Contas já confirmadas (aluguel, fatura, parcela) não precisam de envelope, e a tela diz isso. Sem essa regra, o usuário criaria um envelope para o aluguel e o limite contaria o aluguel duas vezes.
- Envelope maior que o saldo não é bloqueado: a tela mostra quanto não está coberto (`unbacked`), e o limite mostra a falta.
- `/transfers` redireciona para `/envelopes`. Os endpoints de transferência continuam no backend, com seus testes; só a tela saiu.

Descartado: envelope com data de uso. Resolveria "guardar para o IPVA de janeiro", mas duplica o que Metas já faz (valor por mês entrando na projeção) e abre a questão de quando o dinheiro "sai" do envelope.

### Placar de precisão (V21)

- A cada sincronização do Open Finance, depois do commit da importação, `ForecastService.record` guarda uma linha por usuário e dia: o saldo real e, se houver ao menos uma recorrência confirmada, a projeção dos 30 dias seguintes (texto separado por `;`).
- `ForecastAccuracy.compare` avalia cada dia sincronizado contra a previsão mais recente feita **de 7 a 30 dias antes**. Prazos menores ficam de fora porque acertar amanhã é fácil e inflaria a nota.
- Resultado: erro médio em reais, viés (a previsão tende a errar para cima ou para baixo), **negativos evitados** (previsto abaixo de zero, real acima) e **negativos sem aviso** (real abaixo de zero sem previsão negativa). O segundo número existe para o placar não servir só de autoelogio.
- A diferença inclui o que o usuário mudou depois do aviso. A tela diz isso: é para isso que o aviso existe.
- Uma falha ao guardar o placar vira log de aviso e nunca transforma um sync bem-sucedido em erro.

Descartado:
- Guardar no job das 8h. Sem sync automático, o saldo das 8h seria o de dias atrás, e o placar compararia previsão com dado velho.
- Guardar a cada abertura da tela Futuro. Mesma razão, e transformaria uma leitura em escrita.

Limite conhecido: o placar só nasce nos dias em que o usuário sincroniza. Quando o sync virar um job diário, a chamada muda de lugar sem mudar de forma.

### Pix agendado simulado

- No plano de saída, a ação "combinar a nova data" ganha "Preparar Pix". A proposta usa só o que o motor já calculou: destinatário, valor e a nova data (a da próxima entrada).
- O diálogo diz antes e depois da confirmação que é uma simulação e que nada é enviado. Não há endpoint, não há gravação, não há ledger.
- Fatura e empréstimo continuam fora do plano de saída, então nunca ganham Pix para adiar.

## Consequências

- Envelopes dão uso real à antiga Movimentar sem mover dinheiro, então o gatilho do ledger (ADR 0004) continua não disparado.
- A estrela-guia agora tem dado por usuário (`GET /future/accuracy`). O agregado para o time é uma consulta sobre `forecast_snapshot`; não há painel ainda.
- O Pix simulado mede interesse: se as pessoas clicam em "Preparar Pix", o credenciamento ganha argumento com número. Hoje o clique não é contado; contar é o próximo passo barato.
