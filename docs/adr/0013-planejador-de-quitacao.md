# ADR 0013: planejador de quitação sem oferecer crédito

Status: aceito e implementado. 25/09/2026.

## Contexto

Na estratégia (`estrategia-produto.md`), o planejador de quitação é o terceiro item da lista de diferenciais e o primeiro que não depende de dado real que o sandbox não tem. A pessoa com dois ou três empréstimos não sabe onde pôr o dinheiro que sobra. O mercado responde com mais crédito ("junte suas dívidas em um só empréstimo"), e a regra 3 do produto proíbe isso.

## Decisão

`GET /api/v1/future/debt-payoff?extra=<valor>` compara três cenários para os empréstimos com saldo, parcela e taxa (`service/future/DebtPayoff`, Java puro como o resto do motor):

- **Só as parcelas.** Nada muda.
- **Maior juro primeiro (avalanche).** O extra vai para o empréstimo de maior taxa. É a ordem que paga menos juros.
- **Menor saldo primeiro (bola de neve).** O extra vai para o menor saldo. Custa um pouco mais, mas quita uma dívida antes.

Regras da simulação:

1. Juros mensais pela taxa equivalente à anual (`LoanCost.monthlyRate`), arredondados ao centavo todo mês.
2. O orçamento do mês é a soma das parcelas de hoje mais o extra, e fica fixo. Quando uma dívida acaba, a parcela dela vai para a próxima da ordem.
3. O cenário "só as parcelas" não reaproveita nada.
4. Se a parcela não cobre os juros, a resposta diz que não termina, com teto de 600 meses.
5. Empréstimo sem taxa ou sem parcela fica de fora e é contado, para a tela dizer quantos.

O cartão fica de fora. Não sabemos se a fatura está no rotativo, e supor seria contra a regra "perguntar antes de supor".

A tela está em Compromissos, abaixo dos empréstimos. Ela mostra os três cenários, quanto cada ordem economiza e a ordem mais barata com o mês em que cada dívida acaba. Diz também que nenhum crédito é oferecido e que antecipar com redução proporcional dos juros é direito do consumidor (CDC, art. 52, §2º).

## Consequências

- A Wall.ia responde "onde ponho o que sobra" sem vender nada, coerente com o placar de precisão e com a regra 3.
- A simulação ignora IOF e tarifa de antecipação, que o banco pode cobrar ou não. A tela manda pedir ao banco o valor exato.
- Próximo passo, se houver demanda: incluir o cartão quando a pessoa confirmar que está no rotativo, com a taxa do Banco Central (ADR 0010).
