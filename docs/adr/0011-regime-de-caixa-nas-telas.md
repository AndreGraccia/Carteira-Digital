# ADR 0011: Futuro e Análises em regime de caixa, dito na tela

Status: aceito. 25/09/2026.

## Contexto

A auditoria de 24/09 (`auditoria-2026-09-24/openfinance.md`, seção de contabilidade) sugeriu Futuro em regime de caixa e Análises em competência, para explicar por que os números das duas telas não batem.

Conferindo o código em 25/09, as duas telas já são de caixa:

- Futuro projeta o saldo pela data em que o dinheiro sai ou entra. A fatura do cartão pesa no dia do vencimento.
- Análises (`InsightsService`) soma as movimentações das contas pela data de criação (`Transaction.createdAt`, horário de Brasília). As compras do cartão (`card_charge`) não entram. O que entra é o pagamento da fatura, no dia em que ele acontece.

A confusão vinha de outro ponto: quem comprou no cartão em agosto e pagou a fatura em setembro procura a compra em agosto e só a encontra em setembro.

COSIF não se aplica. Ele é o plano de contas das instituições financeiras, e a Wall.ia cuida de finanças de pessoa física.

## Decisão

1. As duas telas continuam em caixa. Para saber se falta dinheiro, o que importa é a data em que ele se move.
2. A descrição da tela Análises diz isso com todas as letras, nas duas línguas: "Compras no cartão entram no dia em que a fatura é paga, não no dia da compra."
3. A competência (compra no mês em que foi feita, parcela distribuída nos meses) fica para uma visão futura de gastos por categoria, que precisará ler `card_charge`.

## Consequências

- Nenhum número muda, e não há migração.
- Quem procurar uma compra do cartão em Análises vai encontrá-la no mês da fatura. O texto da tela avisa.
- Se a visão por competência for feita, ela fica separada e rotulada. Nunca misturada com a de caixa na mesma soma.
