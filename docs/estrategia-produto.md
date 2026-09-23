# Estratégia de produto

Onde o Wall.ia pode ser melhor que tudo o que existe, sem sair do que ele é. Números de mercado vêm de [pesquisa-mercado.md](pesquisa-mercado.md); números de produto vêm do sandbox local, reproduzíveis pelo [roteiro de teste](open-finance-email-tests.md).

## O que o Wall.ia é, em uma frase

**A carteira que mostra o que vai faltar antes de faltar, explica cada número e deixa testar a decisão antes de gastar.**

Três regras seguram essa frase e decidem o que entra e o que fica fora:

1. **Pergunta em vez de supor.** Nada entra na conta sem o usuário confirmar.
2. **Java calcula, a IA no máximo explica.** Nenhum número vem de modelo de linguagem ([ADR 0001](adr/0001-motor-futuro-deterministico.md)).
3. **Neutro.** O Wall.ia não vende crédito. É o que um banco não consegue prometer.

## Diagnóstico: onde o produto estava fraco

Análise feita em 23/09/2026 sobre o código e o sandbox.

| # | Problema | Evidência | Efeito |
| --- | --- | --- | --- |
| D1 | A projeção ignorava o gasto do dia a dia | Mercado Bairro: R$ 2.561,75 em 90 dias, fora das 8 perguntas | Ponto mais baixo mostrado: -R$ 324,87. Real, com o mercado: -R$ 637,93. Erro de R$ 313 para o lado otimista |
| D2 | A tela não respondia "quanto posso gastar?" | Três números de mesmo peso no topo do Futuro | A pessoa fazia a conta de cabeça |
| D3 | O alerta não tinha preço | "Seu saldo pode ficar negativo entre 03/10 e 04/10" | Sem motivo econômico para agir; rotativo a 436% a.a. e cheque especial a até 8% a.m. ficavam invisíveis |
| D4 | Responder uma dúvida exigia navegar por três telas | Futuro, Modo Futuro, Cartões | O próximo passo do README (assistente) não existia |
| D5 | O banco aceitava saldo negativo em carteira local | V8 removeu a restrição para aceitar Open Finance negativo | Regra de dinheiro só no Java; corrigido pela V13 |
| D6 | Login recusava usuário digitado com maiúscula | Cadastro compara ignorando maiúsculas; login comparava exato | Conta criada que não entra; corrigido |

## Mapa de oportunidades

Critérios: **dor** (quanto resolve de um problema com número por trás), **propósito** (reforça as três regras), **diferencial** (existe em algum concorrente da pesquisa?), **esforço** (P, M, G).

| Oportunidade | Dor | Propósito | Diferencial no Brasil | Esforço | Decisão |
| --- | --- | --- | --- | --- | --- |
| Gastos do dia a dia como pergunta | D1: projeção otimista | Pergunta em vez de supor | Mobills e Organizze só têm saldo de fim de mês | M | **Feito** |
| Limite diário até a próxima renda | D2 | Número calculado e explicado | Nenhum player pesquisado | P | **Feito** |
| Custo de não agir (cheque especial) | D3; 29,9% das famílias com conta atrasada | Explica com número | Nenhum player pesquisado | P | **Feito** |
| Pergunte ao Wall.ia, respondido pelo motor | D4 | IA não calcula | Assistentes de WhatsApp registram gasto, não projetam | M | **Feito** |
| Restrição de saldo local no banco | D5 | Dinheiro protegido em duas camadas | Interno | P | **Feito** (V13) |
| Gasto variável no cartão (fatura futura estimada) | Faturas futuras só conhecem compras passadas | Pergunta em vez de supor | Nenhum | M | **Feito** (ADR 0003) |
| Plano de saída do buraco | Alerta ainda não diz o que fazer além de cortar | Explica e sugere, não executa | Nenhum | M | **Feito** (ADR 0003) |
| Alertas proativos por e-mail | A pessoa precisa abrir o app para descobrir | Número calculado chega antes do problema | Nubank avisa cheque especial em outro banco | M | **Feito** (ADR 0003); push fica para depois |
| Metas como compromisso na projeção | README, item 4 | Pergunta em vez de supor | Comum, mas não ligado à projeção diária | M | **Feito** (ADR 0003) |
| Medição e limite do Pergunte | Sem saber o que as pessoas perguntam, não dá para decidir o próximo passo | Mede sem guardar texto | Interno | P | **Feito** (ADR 0003) |
| Classificador de intenção com LLM para o Pergunte | Frases fora do padrão voltam UNKNOWN | IA só escolhe o cálculo | Nenhum | P | Quando a medição pedir |
| Ledger de dupla entrada | Base para dinheiro real | Correção por estorno | Interno | G | Proposto, com gatilho (ADR 0004) |
| Trilha de auditoria administrativa | Admin lista dados de todos sem deixar rastro | Transparência | Interno | P | **Feito** (ADR 0004) |
| Marketplace de crédito | Receita | **Viola a regra 3** | Guiabolso fechou com esse modelo | M | **Não fazer** |
| Registro de gasto por WhatsApp | Menos atrito para anotar | Com Open Finance, anotar à mão é o problema errado | Commodity (Magie, ZapGastos, Focca) | M | **Não fazer** |
| Previsão feita por modelo de linguagem | "Parecer inteligente" | **Viola a regra 2** | Nenhum banco confia nisso para dinheiro | P | **Não fazer** |
| Investimentos e patrimônio | Público amplo | Fora do horizonte curto, onde mora o rotativo | Monarch Plus, bancos | G | Não agora |

## O que foi construído nesta rodada

Detalhe técnico no [ADR 0002](adr/0002-limite-diario-e-perguntas.md).

### Gastos do dia a dia

A Wall.ia calcula a média diária dos últimos 90 dias, fora contas fixas, faturas, empréstimos e dinheiro entre contas próprias, e pergunta: "Você gasta em média R$ 28,46 por dia no dia a dia. Considerar na projeção?". Mostra de onde veio (R$ 2.561,75 em 12 compras, a maior parte no Mercado Bairro). A pessoa confirma, informa outro valor ou recusa.

### Limite diário

A primeira coisa na tela Futuro:

- Com folga (exemplo ilustrativo): "Até 05/10, sua próxima entrada: você pode gastar até R$ 40 por dia. Seu ritmo é R$ 28 por dia, sobram R$ 12 por dia."
- Sem folga (sandbox): "Faltam R$ 324,87 mesmo sem gastar nada. O dia mais apertado é 03/10."

### Custo de não agir

Cada janela negativa diz quanto o cheque especial pode cobrar, no teto legal. No sandbox, com o dia a dia confirmado: até R$ 3,20 de juros em dois dias. O valor é pequeno de propósito: mostra que o problema é o hábito recorrente, não um dia.

### Pergunte ao Wall.ia

Tela nova (`/ask`) com cinco perguntas sugeridas e campo livre. Exemplos reais do sandbox, com as 8 perguntas confirmadas e o dia a dia informado em R$ 20 por dia:

| Pergunta | Resposta |
| --- | --- |
| Posso comprar um notebook de R$ 4.000 em 10x? | Melhor não agora. Menor saldo em 90 dias de -R$ 544,87 para -R$ 944,87; dias negativos de 2 para 6; parcela de R$ 400 no Aurora Card Platinum |
| Quanto posso gastar por dia? | Até 05/10, faltam R$ 324,87 mesmo sem gastar nada |
| Por que meu saldo fica negativo? | Entre 03/10 e 04/10, com a lista das saídas antes do salário |
| Quando cai minha próxima renda? | Concentrix S.A., em 05/10, R$ 4.200,00 |
| Qual a previsão do tempo? | "Ainda não sei responder isso", com a lista do que dá para perguntar |

## Segunda rodada (ADR 0003)

- **Compras do dia a dia no cartão.** No sandbox: R$ 8,30 por dia (Posto Shell, iFood, Spotify em 76 dias), perguntadas como o dia a dia da conta e lançadas na fatura em que cada dia cai.
- **Plano de saída.** No sandbox: "Combinar o pagamento de Imobiliária Lar Seguro (R$ 1.500) de 01/10 para 05/10, depois da entrada. Nenhum dia fica abaixo de zero." Fatura e empréstimo nunca são sugeridos para adiar.
- **Metas.** Até 10, cada uma como compromisso mensal que o limite diário já desconta.
- **Alertas por e-mail.** Opcionais; um por janela negativa nova, às 8h. Verificado ponta a ponta com o Mailpit.
- **Pergunte medido e limitado.** 30 perguntas a cada 10 minutos por pessoa; contagem diária por intenção, sem texto.

## Terceira rodada (ADR 0004 e atualização da plataforma)

- **Trilha de auditoria administrativa:** toda chamada a endpoint de admin, inclusive as negadas, fica registrada com quem, o quê e o resultado, sem corpo nem parâmetros.
- **Spring Boot 3.0.4 para 3.5.16:** a versão antiga estava sem suporte e com CVEs conhecidos. O upgrade encontrou um uso de API do Java 19 que quebraria no JRE 17 de produção.
- **Ledger de dupla entrada, proposto:** hoje nenhum real se move dentro do Wall.ia (carteira local nasce com zero, não há depósito, Open Finance é só leitura). O desenho está pronto no ADR 0004, para quando houver dinheiro em movimento.

## Decisões de produto em aberto

1. **O que fazer com a tela Movimentar.** Carteiras locais nunca recebem dinheiro, então transferir e retirar não têm uso real. Opções: dar um propósito (separar dinheiro em envelopes simulados, que entrariam na projeção) ou retirar a tela. A regra do projeto é não remover funcionalidade sem decisão do dono.
2. **Iniciação de pagamento (Pix pelo Open Finance).** É o que transformaria o Wall.ia de "ver o futuro" em "agir sobre ele" e o gatilho do ledger. Exige credenciamento e muda a regulação aplicável. Decisão de negócio antes de técnica.

## Próximos passos técnicos, em ordem

1. **Classificador de intenção** para o Pergunte, quando a medição mostrar muitas perguntas sem resposta.
2. **Push no navegador** para os alertas, reaproveitando o mesmo job.
3. **Estimativa por cartão** quando houver usuários com mais de um cartão ativo.
4. **Limites em Redis** (login, Gemini, Pergunte) antes de rodar mais de uma instância da API.
5. **Ledger**, no dia do gatilho acima.

## Métricas

| Métrica | O que mede | Por quê |
| --- | --- | --- |
| **Dias no negativo evitados por usuário ativo por mês** (estrela-guia) | Janelas negativas previstas que não aconteceram, comparando projeção e saldo real | É o valor que o produto promete |
| Ativação | Conectou o Open Finance e respondeu pelo menos 3 perguntas em 7 dias | Sem respostas, a projeção não conhece a pessoa |
| Dia a dia confirmado | Porcentagem de usuários com a estimativa confirmada | Mede se a projeção é realista |
| Perguntas sem resposta | Porcentagem de `UNKNOWN` no Pergunte | Diz quando o interpretador precisa crescer |
| Retenção D30 | Voltou 30 dias depois | Referência da pesquisa: PFM freemium converte cerca de 2,1% até o D35 |

## Riscos

| Risco | Mitigação |
| --- | --- |
| Um banco grande copia a projeção diária | A defesa é neutralidade e explicabilidade, não a funcionalidade. Tudo com origem e certeza visíveis |
| A média do dia a dia erra para quem tem gastos irregulares | O usuário pode informar o próprio valor; a certeza aparece como "estimado" |
| O custo do Open Finance real (Pluggy a partir de R$ 2.500 por mês) | Sandbox para desenvolvimento; B2B2C como canal, como sugere a pesquisa |
| Confundir o limite com uma recomendação financeira | Texto sempre descritivo ("pode gastar sem ficar negativo"), nunca prescritivo sobre investimento ou crédito |
