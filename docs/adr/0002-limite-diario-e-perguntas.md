# ADR 0002: limite diário, gastos do dia a dia e perguntas respondidas pelo motor

Status: aceito, 23/09/2026. Complementa o [ADR 0001](0001-motor-futuro-deterministico.md).

## Contexto

O ADR 0001 deixou de fora, de propósito, as compras variáveis (mercado, transporte, farmácia). A tela avisava isso, mas o efeito era maior do que parecia. No sandbox, o "Mercado Bairro" leva R$ 2.561,75 em 90 dias (R$ 28,46 por dia) e não é uma conta fixa, então nunca virava pergunta. Resultado: a projeção dizia que o ponto mais baixo seria -R$ 324,87, e com o mercado ele é -R$ 637,93. A projeção errava para o lado otimista, que é o lado perigoso.

Três outros problemas apareceram na mesma análise:

1. A pergunta que a pessoa faz ao abrir a tela ("quanto posso gastar?") não tinha resposta direta. A tela mostrava três números do mesmo peso (saldo, comprometido, margem livre) e deixava a conta para ela.
2. O alerta de saldo negativo dizia quando e por quê, mas não quanto custa não agir. Com rotativo a 436% ao ano e cheque especial no teto de 8% ao mês, esse custo é o argumento para agir.
3. Responder a uma dúvida exigia navegar entre Futuro, simulador e Cartões. O próximo passo do README era um assistente, e a pesquisa de mercado mostra que os assistentes brasileiros usam IA para registrar gastos, não para projetar.

## Decisão

**Gastos do dia a dia viram uma pergunta, como uma recorrência.**

- `DailySpending` calcula a média por dia dos últimos 90 dias. Ficam de fora: contas fixas já confirmadas ou ainda em pergunta, faturas e parcelas de empréstimo (que vêm dos dados de cartão e empréstimo) e contrapartes com entrada e saída (dinheiro entre contas próprias). Com menos de 28 dias de histórico, não há estimativa.
- A estimativa aparece com a evidência (total, número de compras, para onde foi) e só entra na projeção depois que o usuário confirma. Ele pode informar outro valor. A resposta fica em `recurrence_decision` com `pattern_key = 'daily-spending'` e o valor opcional em `amount` (V14).
- Na projeção, vira um evento `ESTIMATED` por dia a partir de amanhã (`Kind.DAILY_SPENDING`); o saldo de hoje já reflete o que foi gasto hoje. Nas listas, os eventos diários aparecem somados numa linha só.
- Não entra em "já tem destino": esse número é só contas, faturas e parcelas. Mercado futuro estimado não tem destino ainda.
- Sem histórico suficiente, a pergunta continua aparecendo e pede o valor por dia.

**O limite diário é a primeira resposta da tela Futuro.**

- `SpendingLimit` projeta só as contas (sem o dia a dia) até a próxima renda confirmada (ou 30 dias) e acha o maior `d` tal que `saldo(t) - d * t >= 0` para todo dia `t` de amanhã (`t = 1`) até a véspera da renda. Devolve o dia que define o limite (`bindingDate`).
- Se só as contas já passam do saldo, o limite é zero e a resposta é quanto falta (`shortfall`), com o dia em que falta mais.
- Com o gasto do dia a dia confirmado, a tela compara o ritmo com o limite: "sobram R$ X por dia" ou "gaste R$ Y a menos por dia".

**Cada janela negativa diz quanto pode custar.**

- `overdraftCostCap`: soma, dia a dia, do saldo negativo vezes a taxa diária equivalente a 8% ao mês ((1,08)^(1/30) - 1 = 0,25687% ao dia), o teto do cheque especial pela Resolução CMN 4.765/2019. É um limite superior e a tela diz isso.

**Pergunte à Wall.ia responde com o motor, não com um modelo de linguagem.**

- `QuestionParser` reconhece cinco intenções (compra, limite por dia, por que negativo, o que já tem destino, próxima renda), tira valor e parcelas do texto e escolhe o cálculo. Pergunta fora disso volta `UNKNOWN`, com a lista do que dá para perguntar.
- A resposta (`POST /api/v1/future/ask`) traz só números; a frase é montada na interface com o i18n. Compra usa o mesmo simulador do Modo Futuro e ganha um veredito: `RISK` (cria dias negativos ou aprofunda um buraco que já existe), `TIGHT` (sobra menos de uma semana de gasto do dia a dia no ponto mais baixo) ou `SAFE`.
- O interpretador prefere não achar valor a achar o errado: datas ("dia 25"), anos ("natal de 2026") e números de modelo ("iPhone 15") não contam como preço. Sem valor claro, a resposta pede o valor. "Pagar a fatura" é tratado como pergunta sobre o que já tem destino, nunca como compra nova.

## Alternativas consideradas

| Alternativa | A favor | Contra |
| --- | --- | --- |
| Gasto do dia a dia por categorias (mercado, transporte) | Mais preciso por tipo | Exige categorização confiável de texto livre; a média total já corrige o maior erro |
| Incluir o dia a dia automaticamente, sem perguntar | Menos um clique | Quebra a regra do produto: nada entra na conta sem o usuário confirmar |
| LLM interpretando e respondendo as perguntas | Entende qualquer frase | Pode inventar número; custo por pergunta; manda dados para fora. Fica como opção só para classificar a intenção |
| Custo com os juros médios do banco do usuário | Número mais próximo do real | Não há essa taxa no Open Finance de dados; o teto legal é conhecido e defensável |

## Consequências

Ganhamos:

- A projeção deixa de ser otimista quando o usuário confirma o dia a dia. As regras estão travadas em `DailySpendingAndLimitTest`, `QuestionParserTest` e `FutureVerdictTest`; os números do sandbox são reproduzíveis pelo roteiro de API.
- A tela Futuro responde primeiro o que a pessoa veio perguntar.
- O alerta vira decisão com preço.
- Um assistente sem alucinação e sem custo por pergunta. Se um modelo de linguagem for plugado depois, ele só escolhe a intenção; os números continuam vindo do Java.

Perdemos, ou ficou para depois:

- Compras variáveis feitas no cartão (iFood, combustível) continuam fora: as faturas futuras só conhecem as compras já feitas. É o próximo passo natural (estimativa por cartão).
- A média trata todos os dias igual. Quem gasta mais no fim de semana vê um limite "médio".
- O `QuestionParser` entende as formas mais comuns em português e inglês. Frases muito diferentes voltam `UNKNOWN`, o que é seguro, mas frustra. Medir quais perguntas caem em `UNKNOWN` antes de trocar por um classificador.
- Heurística de contas próprias: uma pessoa que te paga e recebe de você também sai da estimativa.

## Revisão

Uma revisão adversarial encontrou 11 problemas antes da entrega; 10 foram corrigidos com teste:

| Problema | Correção |
| --- | --- |
| "iPhone 15 em 12x" virava compra de R$ 15, veredito "pode, com folga" | Número só conta como preço com R$, "mil"/"reais" ou uma palavra de preço antes; sem valor claro, pede o valor |
| Ano e dia do mês lidos como valor | Datas removidas antes; ano só vale se não houver outro candidato |
| "Posso pagar a fatura?" simulava uma compra nova no cartão | Vira pergunta sobre compromissos |
| "Limite do cartão" caía no limite diário; "lowest" casava "owe" | Palavras com fronteira e frases específicas |
| Comprar com o saldo já negativo nos 91 dias dava "apertado" | Aprofundar um buraco existente é `RISK` |
| O dia apontado no "faltam R$ X" não era o dia em que faltava X | Com falta, o dia é o do menor saldo |
| O gasto de hoje contado duas vezes | Gasto diário começa amanhã |
| "Já tem destino" somava mercado futuro estimado | Fica só com contas, faturas e parcelas |
| Sem histórico, não havia como informar o gasto diário | A pergunta aparece e pede o valor |
| "Pagamento cartão Nubank" rejeitado como recorrência voltava na média | Filtro de cartão e empréstimo ampliado |

Ficou para depois: limite de requisições no `/ask` (hoje cada compra lê o histórico duas vezes).

## Como reverter

- O gasto do dia a dia só entra com a decisão `daily-spending` confirmada; apagar essas linhas de `recurrence_decision` volta a projeção ao comportamento do ADR 0001.
- `SpendingLimit`, `DailySpending` e `QuestionParser` são classes sem Spring; removê-las e os campos `dailySpending`, `spendingLimit` e `overdraftCostCap` da resposta não mexe em carteiras, transações nem Open Finance.
- A V14 só acrescenta uma coluna opcional.
