# ADR 0010: taxas e indicadores do Banco Central no custo do negativo

Status: aceito e implementado no backend. 25/09/2026.

## Contexto

Até aqui, o custo de uma janela negativa era calculado sempre no teto legal do cheque especial, 8% ao mês (Res. CMN 4.765/2019), sem dizer se é isso que o banco do usuário cobra e sem o IOF. O Banco Central publica a taxa média de cada banco. Na janela de 04/09/2026, 29 bancos publicaram a taxa do cheque especial: ela vai de menos de 1% a 8,27% ao mês, e a mediana fica em 8,12%. Quem está nos bancos grandes paga, na prática, o teto. Quem está num banco com taxa baixa paga bem menos.

O Banco Central publica dados abertos, sem chave:

- **Olinda `taxaJuros/v2`, `TaxasJurosDiariaPorInicioPeriodo`**: taxa média por instituição (`cnpj8`), modalidade e segmento, em janelas de 5 dias úteis. Os campos usados são `InicioPeriodo`, `FimPeriodo`, `Segmento`, `Modalidade`, `InstituicaoFinanceira`, `TaxaJurosAoMes`, `TaxaJurosAoAno` e `cnpj8`.
- **SGS**: 4389 (CDI), 13522 (IPCA em 12 meses) e 432 (Selic meta).

## Decisão

- **Cache diário no banco (V29).**
  - `market_rate` guarda a janela mais recente de pessoa física em três modalidades: cheque especial, rotativo total do cartão e crédito pessoal não consignado, todas prefixadas.
  - `market_indicator` guarda a última observação de cada série do SGS.
  - O job roda às 6h (`market_refresh_cron`) com o `JobLock`. Numa instalação nova, também roda uma vez na subida, se a tabela estiver vazia (`market_refresh_on_startup`).
- **Falha não quebra tela.**
  - A busca HTTP fica fora da transação.
  - Cada parte (Olinda e cada série do SGS) falha sozinha, e fica valendo o último valor guardado.
  - Sem nenhum dado guardado, o custo volta ao teto legal.
- **Taxa usada**, em ordem:
  1. a do banco do usuário, casando os 8 primeiros dígitos do `institutionId` da conexão com o `cnpj8` (fonte `BANK`);
  2. a mediana do mercado (`MARKET`), escolhida em vez da média porque poucos bancos com taxa muito alta puxariam a média para cima;
  3. o teto legal (`LEGAL_CAP`).

  Nenhuma taxa passa do teto de 8% a.m.
- **Cálculo.**
  - O motor continua calculando o juro no teto (`overdraftCostCap`).
  - Como o juro é linear no saldo negativo diário, `OverdraftCost` recupera a soma "saldo × dias" a partir desse valor e aplica a taxa diária composta equivalente, com a mesma convenção de 30 dias do teto.
  - IOF de pessoa física: 0,0082% ao dia sobre o saldo usado, mais 0,38% sobre o pico do negativo.
  - O resultado sai em `riskWindows[].overdraftEstimate`, com fonte, instituição e data de referência ("de onde veio este número").
- **E-mail de alerta**: mostra a estimativa com IOF e a fonte da taxa, e o teto legal como limite máximo.
- **`GET /api/v1/market/overview`**: devolve mediana, menor e maior taxa e a taxa do banco do usuário por modalidade, mais CDI, IPCA e Selic, para a tela de compromissos e as metas.

## Consequências

- A estimativa do IOF usa o pico do negativo como base dos 0,38%. O banco cobra essa parte sobre cada aumento do saldo devedor, então o valor real pode variar um pouco.
- A taxa do Olinda é a **média praticada** pela instituição, não a do contrato do usuário. A tela diz isso.
- A média publicada pode passar de 8% a.m. (é o caso da mediana em setembro de 2026), porque inclui encargos além dos juros. O app limita os juros no teto legal e mostra o IOF à parte.
- O sandbox de participante não tem CNPJ no `institutionId` e sempre usa a mediana.
- As janelas antigas ficam na tabela, cerca de 150 linhas por dia. Isso serve de histórico; se crescer demais, limpa-se o que tiver mais de um ano.

## Descartado

- **Consultar o BCB a cada requisição**: o serviço é lento e cai às vezes. Em 24/09 a série 432 respondeu 502.
- **Média simples**: é distorcida por poucas instituições com taxa alta.
- **`TaxasJurosMensalPorMes`**: só cobre financiamento imobiliário.
