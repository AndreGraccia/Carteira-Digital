# Wall.ia: pesquisa de mercado

Data de corte: 22/09/2026. Legenda de fonte: **[P]** primária (órgão, entidade setorial, empresa sobre si mesma); **[S]** secundária (imprensa, blog, agregador). Quando o número vem de notícia que cita a fonte primária, está marcado [S] com a fonte original entre parênteses. "Não encontrado" significa que procurei e não achei fonte pública confiável.

---

## Resumo executivo

- **A base endereçável já está conectada.** O Open Finance tinha **208,79 milhões de consentimentos ativos** em 31/07/2026 e **138,46 milhões de PFs únicas na recepção de dados** em 13/08/2026 [S, citando dashboard OFB]. O Pix tem **~170 milhões de usuários PF** em 2026 [S, citando BCB]. **96,4% dos adultos** têm conta [S, citando Relatório de Cidadania Financeira do BCB].
- **A dor é recorde.** **82% das famílias endividadas** e **29,9% com contas em atraso** (Peic/CNC, ago/2026) [S]. **83,9 milhões de inadimplentes** (51% dos adultos) no Serasa (ago/2026) [S]. O comprometimento de renda com serviço da dívida bateu **28,85%** (jun/2026), máximo da série do BCB [P].
- **O cartão é o centro do problema.** Presente em **84,6% das dívidas das famílias** (Peic mai/2026) [P]. Rotativo a **436,15% a.a.** (jul/2026, BCB série 22022) [P]. **42,6%** das compras no crédito são parcelado sem juros (Abecs 2025) [S], o que cria o compromisso futuro invisível que o Wall.ia quer mostrar.
- **O mercado de PFM independente no Brasil foi comprado pelos bancos.** Guiabolso (6 mi de usuários) foi comprado pelo PicPay por R$ 150–200 mi e fechado em 2022. Olivia foi comprada pelo Nubank e fechada em 2022. Mobills (10 mi de usuários) foi comprado pela Toro/Santander em 2021. Hoje Nubank, PicPay, Itaú, Inter, Bradesco e BTG agregam contas de outros bancos de graça.
- **Preços no Brasil:** Mobills Premium sai a **R$ 99,90/ano**, Organizze a **R$ 35–69/mês**, assistentes de IA no WhatsApp **a partir de R$ 9,90/mês**. Fora: YNAB US$ 109/ano, Copilot US$ 95/ano, Monarch US$ 99,99–199/ano.
- **O custo de entrada do Open Finance é o gargalo da unit economics.** A Pluggy começa em **R$ 2.500/mês** só para dados [P]. Um PFM freemium converte em torno de **2,1% dos downloads em pagantes até o D35** (RevenueCat 2026) [P]. Com esses números, CAC pago não fecha: é preciso crescer por canal orgânico ou por B2B2C.
- **Espaço vazio:** não encontrei nenhum player brasileiro que junte projeção de saldo **diária** com menor saldo previsto, simulação de cenários e explicabilidade rastreável. Mobills e Organizze têm só "saldo previsto" de fim de mês. A referência internacional mais próxima é o **Monarch Plus** (US$ 199/ano, lançado em abr/2026), que faz forecasting de fluxo de caixa e simulações.

---

## 1. Tamanho do mercado no Brasil

| Métrica | Valor | Data ref. | Fonte |
|---|---|---|---|
| Adultos com conta (bancarização) | 96,4% | Relatório abr/2026 | [S] Poder360, citando BCB Relatório de Cidadania Financeira |
| Adultos com relacionamento no SFN | 175,22 mi (2024); 88% são usuários ativos; >21 mi sem atividade | 2024 | [S] idem; [P] BCB RCF 2025 |
| Usuários PF do Pix | 148,3 mi em 2025; ~170 mi em 2026 (~80% da população) | 2025/2026 | [P] BCB Relatório de Gestão do Pix; [S] para o número de 2026 |
| Chaves Pix | 920 mi | fim de 2025 | [S] O Tempo, citando BCB |
| Consentimentos ativos Open Finance | 208,79 mi | 31/07/2026 | [S] Let's Money, citando dashboard OFB |
| Consentimentos ativos (referência anterior) | 154 mi | fev/2026 | [S] Finsiders, citando Dashboard do Cidadão OFB |
| PFs únicas, recepção de dados | 138,46 mi | 13/08/2026 | [S] Let's Money, citando OFB |
| PFs únicas, transmissão de dados | 129,78 mi | 13/08/2026 | [S] idem |
| Clientes/contas conectados | >100 mi | fev/2026 | [S] Finsiders, citando OFB |
| Chamadas de API (dados abertos) | 7,14 bi | 03/08/2026 | [S] Let's Money |
| Iniciações de pagamento via OF (acumulado) | 134,41 mi | 13/08/2026 | [S] Let's Money |
| Transações bancárias totais | 240,8 bi, sendo 78% pelo celular (187,5 bi) e 83% em canais digitais | 2025 | [P] Pesquisa Febraban de Tecnologia Bancária 2026 |
| Clientes com >80% das transações no digital | 76% | 2025 | [P] idem |
| Acessos à tela de "outros bancos" no Nubank | ~8 mi/mês | 2024/2025 | [S] Finsiders, citando Nubank |
| Usuários de apps de controle financeiro (agregado BR) | não encontrado | – | – |

Leitura: o funil de Open Finance para PF (~138 mi de PFs únicas recebendo dados) já está quase do tamanho da base do Pix. Não falta infraestrutura nem hábito de consentir. O que falta é um motivo para o usuário conectar as contas num app que não é o banco dele.

Nota: o site openfinancebrasil.org.br devolveu 403 quando tentei acessar direto. Os números do dashboard vêm de notícias que o citam. Antes de publicar, confira no Dashboard do Cidadão.

---

## 2. Dor: endividamento, inadimplência e cartão

| Métrica | Valor | Data ref. | Fonte |
|---|---|---|---|
| Famílias endividadas (Peic) | 82,0% (recorde pelo 7º mês seguido) | ago/2026 (publ. 10/09/2026) | [S] Portal do Comércio (CNC) |
| Famílias com contas em atraso | 29,9% | ago/2026 | [S] idem |
| Sem condições de pagar as dívidas atrasadas | 12,5% | ago/2026 | [S] idem |
| "Muito endividadas" | 17,4% | ago/2026 | [S] idem |
| Tempo médio de atraso | 65 dias | ago/2026 | [S] idem |
| Renda até 3 SM: endividadas / em atraso / sem condições | 85,1% / 38,8% / 18,3% | ago/2026 | [S] idem |
| Cartão de crédito entre os tipos de dívida | 84,6% das famílias endividadas | mai/2026 | [P] Peic mai/2026 (PDF CNC) |
| Famílias com mais de 50% da renda comprometida com dívidas | 18,7% | mai/2026 | [P] idem |
| Comprometimento médio da renda (Peic) | 29,3% | mai/2026 | [P] idem |
| Inadimplentes Serasa | 83,9 mi (51% dos adultos) | ago/2026 | [S] Bloomberg Línea / O Povo, citando Serasa |
| Composição das dívidas Serasa | cartão 27,2%; contas básicas 21,1%; financeiras 19,9% | ago/2026 | [S] idem |
| Dívida média por inadimplente | R$ 6.598,13 | 2026 | [P] Serasa, 10 anos do Mapa |
| Comprometimento de renda com serviço da dívida (BCB, com ajuste sazonal) | 28,85% (máximo da série) | jun/2026 | [P] BCB SGS 29034 |
| Endividamento das famílias sobre a renda de 12 meses | 49,75% | jun/2026 | [P] BCB SGS 29037 |
| Juros do rotativo, cartão PF | 436,15% a.a. | jul/2026 | [P] BCB SGS 22022 |
| Juros do cartão parcelado | 189,3% a.a. | jul/2026 | [S] InfoMoney, citando BCB |
| Selic meta | 13,75% a.a. | set/2026 | [P] BCB SGS 432 |
| Volume transacionado em cartões | R$ 4,5 tri (+10,1%); crédito R$ 3,1 tri (+14,5%) | 2025 | [S] Finsiders/CNN, citando Abecs |
| Parcelado sem juros no crédito | 42,6% das compras (à vista 57,1%); 64,2% em até 6x | 2025 | [S] Panorama Abecs |
| Teto de juros do rotativo/parcelamento da fatura | 100% do valor original (Lei 14.690/2023) | desde jan/2024 | [S] InfoMoney |

Implicação para o produto: o parcelado sem juros é crédito que não parece crédito. Com 42,6% do volume do crédito parcelado e o cartão em 84,6% das dívidas, a pergunta "quanto do meu saldo futuro já está comprometido?" tem público de massa. O rotativo a 436% a.a. torna o alerta "entre os dias 3 e 5 seu saldo pode ficar negativo" economicamente valioso, porque evitar um único rotativo paga a assinatura de um ano.

---

## 3. Concorrência

### 3.1 PFMs independentes no Brasil

| Player | Situação | Preço | Escala | Projeção / cenários / explicabilidade | Fonte |
|---|---|---|---|---|---|
| Mobills | Comprado pela Toro (Santander) em 2021 | Premium R$ 99,90/ano (~R$ 8,40/mês); tem plano PRO com IA no WhatsApp (preço não listado na página) | ">10 mi de usuários" (acumulado) | "Saldo previsto" de fim de mês; há queixa pública de bug nele. Sem simulação de cenários. | [P] mobills.com.br/pricing; [P] Santander Imprensa; [S] Reclame Aqui |
| Organizze | Independente | Manual R$ 35/mês; Conectado R$ 45/mês ou R$ 399,90/ano; Plus R$ 69/mês ou R$ 599,90/ano | não encontrado | Projeção por lançamentos recorrentes; Open Finance só nos planos conectados | [P] organizze.com.br/planos |
| Minhas Economias | Grupo Valemobi | Gratuito, monetizado com publicidade e parcerias; clube pago opcional | não encontrado | Open Finance; sem simulação | [S] napratica.org.br; [P] site |
| Meu Dinheiro | Independente, com foco PF e PME | Tem plano gratuito; preços de PF não capturados | não encontrado | Fluxo de caixa e contas a pagar/receber (estilo ERP) | [P] meudinheiroweb.com.br |
| Minhas Finanças (app) | Independente | Planos "Sync Bank" e "Sync Bank Pro" (preço não capturado) | não encontrado | Open Finance e IA | [P] minhasfinancas.app.br |
| Guiabolso | Comprado pelo PicPay em jul/2021 por R$ 150–200 mi (NeoFeed); app encerrado em nov/2022 | Era gratuito, monetizado com marketplace de crédito (R$ 1 bi concedido via parceiros) | 6 mi de usuários | – | [S] Exame, Seu Dinheiro, Startups |
| Olivia (IA) | Anúncio de compra pelo Nubank em nov/2021; fechamento em 03/01/2022; app encerrado em 15/07/2022 | – | não divulgado | Sugestões por IA | [S] Exame, Finsiders |

**Por que o Guiabolso acabou:** o modelo era PFM gratuito que monetizava vendendo crédito. Com o Open Finance, qualquer banco passou a poder montar a mesma visão consolidada dentro do próprio app. O PicPay comprou "para ser protagonista do open banking e acelerar o marketplace financeiro" [S, Consumidor Moderno/Exame], absorveu as funcionalidades no "Minhas Finanças" do PicPay e desligou o app. A lição para o Wall.ia: agregação sozinha virou commodity, e quem monetiza vendendo crédito entra em conflito com a promessa de "controle".

### 3.2 Assistentes de IA (principalmente via WhatsApp), 2025-2026

| Player | Proposta | Preço | Escala | Fonte |
|---|---|---|---|---|
| Magie | Pix e assistente financeiro por áudio ou texto no WhatsApp; migrando para B2B | não encontrado (B2C) | 400 mil usuários no fim de 2025; >1 mi em ago/2026, após onboarding via banco parceiro; captou US$ 5 mi em jan/2026 (total >US$ 10 mi) | [S] Finsiders, Let's Money |
| Financinha | IA no WhatsApp; Open Finance com >130 instituições; "antecipa riscos" | não encontrado | >38 mil usuários (jul/2026) | [P] site / blog |
| ZapGastos | Registro de gastos por WhatsApp | não encontrado | >17 mil ativos | [P] site |
| Focca-Ai | WhatsApp: contas a pagar, lembretes, metas | a partir de R$ 9,90/mês | não encontrado | [P] focca-ai.com |
| Poupa.ai, GranaZen, MeuControle.IA | Assistentes via WhatsApp | não encontrado | não encontrado | [P] sites |

Padrão da categoria: a IA é usada para **registrar** gastos com menos atrito, não para **projetar** saldo. Ninguém nessa lista se posiciona em cálculo determinístico explicável. O pivot da Magie para B2B indica que monetizar B2C é difícil.

### 3.3 PFM dentro dos bancos (grátis para o cliente)

| Banco | Recurso | Desde | Fonte |
|---|---|---|---|
| Nubank | Tela com saldo e extrato de outros bancos (~8 mi acessos/mês); alertas de dinheiro parado e de cheque especial em outro banco; "Transferências Inteligentes" via iniciação de Pix; R$ 6,4 mi em juros economizados em 10 meses | 2023+ | [S] Finsiders; [P] blog Nubank |
| PicPay | "Minhas Finanças": agregação e categorização via OF; agregador de investimentos | fim de 2022; investimentos em 2026 | [S] Exame, InfoMoney, Let's Money |
| Itaú | "Minhas Finanças" (2019); agregador de investimentos no íon | 2019+ | [S] substack Tiago Carvalho; [P] ion.itau |
| Inter | "Bússola"; "Trazer Dinheiro" via OF | ago/2023 | [S] idem; [P] inter.co |
| Bradesco (Next) | "Minhas Finanças" | ago/2022 | [S] idem |
| BTG | "Finanças+" | jan/2023 | [S] idem |

A fraqueza estrutural do PFM de banco: a agregação existe para **trazer o dinheiro para dentro de casa** (portabilidade, cross-sell). Nenhum banco tem incentivo a mostrar ao cliente que ele deve evitar o crédito do próprio banco. Essa é a brecha de "confiança" e "Explainable Finance" que o Wall.ia pode ocupar. Ressalva: não encontrei evidência pública de que algum banco faça projeção diária de saldo com cenários.

### 3.4 Referências internacionais

| Player | Preço | Escala / receita | Projeção e cenários | Fonte |
|---|---|---|---|---|
| Monarch Money (EUA) | Core US$ 14,99/mês ou US$ 99,99/ano; Plus US$ 199/ano (lançado em abr/2026) | Série B de US$ 75 mi com valuation de US$ 850 mi (mai/2025); ~500 mil pagantes e US$ 12,6 mi de ARR segundo estimativas de terceiros (Latka/Sacra, baixa confiabilidade) | **Sim, no Plus:** forecasting de fluxo de caixa e patrimônio, com what-ifs (aposentadoria, compra de casa, pausa na carreira) | [P] help.monarch.com; [S] TFN, Penny Hoarder, Latka |
| Copilot Money (EUA, iOS) | US$ 13/mês ou US$ 95/ano, por pessoa | não encontrado | **Não:** o Cash Flow mostra só o passado; forecasting ainda é pedido de usuário no roadmap | [P] help.copilot.money, roadmap |
| YNAB | US$ 14,99/mês ou US$ 109/ano (até 6 pessoas) | não encontrado | Método de orçamento; sem projeção diária | [S] Finny, PocketClear |
| Cleo (EUA/UK, IA) | Assinaturas Plus/Builder, cash advance, cartão | ARR de US$ 280 mi (jul/2025), >US$ 300 mi depois; >1,1 mi de pagantes; lucrativa desde ago/2024 | Chat com IA; receita vem muito de cash advance (crédito) | [S] Sacra, Redbus |
| Rocket Money (Rocket Cos.) | US$ 6–12/mês (o usuário escolhe); negociação de contas cobra 35–60% da economia do 1º ano | ">10 mi de membros" | Foco em assinaturas e contas | [P] help.rocketmoney.com; [S] Clark |
| Emma (UK) | Plus £4,99; Pro £9,99; Ultimate £14,99 por mês | não encontrado (estimativa de app intelligence: ~US$ 20 mil/mês, não confiável) | Detecção de assinaturas | [P] help.emma-app.com |

### 3.5 O espaço vazio

| Capacidade | Mobills/Organizze | Bancos BR | IA WhatsApp BR | Monarch Plus | Copilot | Wall.ia (proposta) |
|---|---|---|---|---|---|---|
| Agregação via Open Finance | Sim (pago) | Sim (grátis) | Alguns | Sim (Plaid etc.) | Sim | Sim |
| Recorrência detectada **e confirmada pelo usuário** | Parcial (lançamento recorrente manual) | não encontrado | não encontrado | Recorrentes automáticos | Recorrentes automáticos | Sim |
| Saldo comprometido (parcelas futuras) | Parcial (fatura) | não encontrado | não | parcial | parcial | Sim |
| Projeção **diária** com menor saldo e janela de risco | Não (só fim de mês) | não encontrado | "antecipa riscos" (Financinha, sem detalhe) | Sim (semanas/meses) | Não | Sim |
| Simulador de cenários sem mexer no dinheiro | Não | Não | Não | Sim (longo prazo) | Não | Sim (curto e médio prazo) |
| IA só explica número calculado (read-only), com trilha | Não | Não | Não (IA é a interface principal) | não encontrado | não encontrado | Sim |
| Sem conflito de interesse com venda de crédito | Mobills pertence a banco | Não | Magie é B2B/bancos | Sim | Sim | Sim (se sustentado) |

Conclusão: no Brasil, a combinação **projeção diária + cenários + explicabilidade** não aparece em nenhum player pesquisado. O Monarch Plus prova que existe gente disposta a pagar por forecasting (US$ 199/ano, o dobro do plano base). A diferença do Wall.ia é o horizonte curto (dias e semanas, que é onde mora o rotativo) e o contexto brasileiro (parcelado sem juros e fatura). Risco: um banco grande pode copiar a projeção; a defesa é a neutralidade e a explicabilidade, não a feature.

---

## 4. Modelo de negócio e unit economics de referência

### 4.1 Preços

| Produto | Preço mensal equivalente | Fonte |
|---|---|---|
| Mobills Premium | R$ 8,33 (R$ 99,90/ano) | [P] |
| Organizze Conectado | R$ 33,33 (anual) a R$ 45 (mensal) | [P] |
| Organizze Conectado Plus | R$ 49,99 (anual) a R$ 69 (mensal) | [P] |
| Focca-Ai | a partir de R$ 9,90 | [P] |
| Copilot / YNAB / Monarch Core (anual) | US$ 7,92 / US$ 9,08 / US$ 8,33 | [S]/[P] |
| Monarch Plus | US$ 16,58 (US$ 199/ano) | [S] |

### 4.2 Conversão, retenção e CAC

| Métrica | Valor | Fonte |
|---|---|---|
| Download para pago até o D35, freemium (mediana, todas as categorias) | 2,1% | [P] RevenueCat, State of Subscription Apps 2025/2026 |
| Download para pago até o D35, hard paywall | 10,7% | [P] idem |
| Trial para pago com trial de 17–32 dias | 42,5% (vs 25,5% com trial de menos de 4 dias) | [P] RevenueCat 2026 (resumo no blog) |
| Cancelamento de assinantes anuais | 72% cancelam em até 1 ano; 35% dos cancelamentos no mês 1 | [P] RevenueCat 2026 (resumo no blog; conferir definição no relatório) |
| Retenção de apps com IA | 36% pior em 12 meses que apps tradicionais | [P] idem |
| Recorte específico para Finanças ou LatAm | não encontrado no resumo público | – |
| CAC de apps de finanças pessoais no Brasil | não encontrado | – |
| Abandono de apps financeiros | "70% abandonam em 3 meses" (afirmação de marketing sem fonte primária; não usar como dado) | [S] Focca-Ai |

### 4.3 Custo de agregador Open Finance

| Fornecedor | Preço público | Observação | Fonte |
|---|---|---|---|
| Pluggy | Dados a partir de **R$ 2.500/mês**; Pagamentos (Pix, Pix Automático, sweeping) a partir de **R$ 500/mês**; excedente cobrado por requisição (valor não publicado); trial de 15 dias em produção; sandbox incluso | Inclui categorização e **detecção de recorrência**. A Pluggy se declara ITP autorizada pelo BCB. | [P] pluggy.ai/precos |
| Belvo | ~R$ 6.000/mês (relato de desenvolvedor, não oficial) | Página oficial não mostra o preço BR | [S] TabNews |
| Tecnospeed | R$ 1.500 de entrada + R$ 540/mês (relato) | – | [S] TabNews |
| Revenda sobre Pluggy ("Banco MCP") | a partir de R$ 19,90/mês por conta conectada | Mostra o preço de varejo por conexão | [S] TabNews |
| Klavi | não encontrado | – | – |
| Custo por conexão ativa no atacado | não encontrado (sob contrato) | – | – |

Implicação: um piso de R$ 2.500/mês equivale a **~300 assinantes a R$ 8,50 líquidos** só para pagar o agregador. No freemium, conectar Open Finance para usuário gratuito tem custo marginal desconhecido e cobrado por excedente. É preciso limitar conexões no plano grátis ou negociar preço por conta ativa.

### 4.4 Como receber dados do Open Finance

- Participar **diretamente** como receptora de dados exige ser **instituição autorizada a funcionar pelo BCB** (IF, IP ou ITP) e cumprir requisitos técnicos e de segurança do ecossistema. Base: Resolução Conjunta nº 1/2020 e alterações [P].
- Empresa não autorizada participa **indiretamente, em parceria com instituição autorizada** (agregador/ITP como Pluggy ou Belvo). A receptora precisa identificar o cliente e obter consentimento antes do compartilhamento [P, Res. Conj. 1/2020; S, Pluggy/NDM Advogados].
- Para o Wall.ia hoje (sandbox + dados simulados), o caminho realista é **parceria com agregador**. Licença própria de ITP é projeto de capital regulatório e compliance, fora do escopo acadêmico.

---

## 5. Regulatório

| Tema | Ponto relevante para o Wall.ia | Fonte |
|---|---|---|
| **LGPD (Lei 13.709/2018)** | Base legal para tratar os dados (art. 7º: consentimento ou execução de contrato); direitos do titular, como acesso, correção, portabilidade e eliminação (art. 18); **revisão de decisões automatizadas** (art. 20), que se aplica se a IA ou o motor influenciarem decisões sobre o usuário; segurança (art. 46); comunicação de incidente à ANPD (art. 48). Dado financeiro não é "sensível" pelo art. 5º, mas o sigilo bancário (LC 105/2001) vale para as instituições. | [P] planalto.gov.br |
| **Resolução Conjunta nº 1/2020 (CMN/BCB)** | Regula o Open Finance: consentimento com finalidade, prazo e escopo definidos, revogável; papéis de transmissora, receptora e iniciadora; só instituições autorizadas participam diretamente | [P] normativos.bcb.gov.br |
| **Papel de receptora** | Instituição autorizada que recebe dados com consentimento. Não autorizada só via parceiro. | [P]/[S] acima |
| **Pix Automático** (lançado em jun/2025) | Débito recorrente autorizado pelo pagador no Pix. Para o Wall.ia, é uma fonte **explícita** de recorrência: o usuário já autorizou, então o compromisso futuro é conhecido e não só inferido. Números: 919 mil transações em jan/2026 e ~2,87 mi em mai/2026 (a unidade aparece de forma inconsistente na fonte; conferir no BCB); R$ 1,2 bi em mai/2026; R$ 4,5 bi acumulados em 2026. Adoção ainda baixa: exige saldo (sem cheque especial), autorização prévia e educação do usuário. | [S] Finsiders (16/06/2026), citando BCB |
| **Drex** | O BCB desligou a plataforma piloto em DLT (Hyperledger) por problemas de privacidade e escala. A fase de 2026 é **sem blockchain**, focada em reconciliação de gravames (usar ativos como garantia de crédito). Sem impacto no escopo do Wall.ia. | [S] Metrópoles, Exame, Seu Dinheiro |
| **Portabilidade de crédito via OF** | Resolução Conjunta nº 15/2025 e Resolução CMN nº 5.265. Operando desde 02/02/2026 para crédito pessoal sem garantia; consignado público federal em teste desde ago/2026, com lançamento previsto para nov/2026; imobiliário depois. Conclusão em até 5 dias úteis. | [S] Poder360, Agência Brasil, Pluggy |
| **JSR (Jornada sem Redirecionamento)** | Obrigatória desde 06/02/2026 para quem tem conta Pix (Resolução BCB nº 541/2025); piloto 2.2.0 até 21/04/2026 (Pix Automático, desktop, PJ). É a base do **Pix por aproximação** (NFC). | [S] Sensedia, Pluggy; [P] Área do Desenvolvedor OFB |

---

## 6. Tendências 2026

| Tendência | Dado | Fonte |
|---|---|---|
| IA agêntica nos bancos | Febraban Tech 2026 (24–26/08, tema "Agentes Inteligentes, Liderança Humana"). Investimento em tecnologia de R$ 50,4 bi em 2026 (+8%), sendo R$ 3 bi em IA generativa; IA generativa e cloud citadas como prioridade por 84% das instituições; cibersegurança por 100%. | [S] Brasil Inovador/Celcoin, citando Pesquisa Febraban/Deloitte 2026 |
| Open Finance, fase atual | Da escala para governança (responsabilização, rastreabilidade); portabilidade de crédito; jornada otimizada (dois consentimentos numa aprovação); OF PJ com fluxo assíncrono (CIBA) | [S] Let's Money, Pluggy |
| Pagamentos via OF | 134,41 mi de iniciações acumuladas; JSR obrigatória; Pix por aproximação | [S] |
| Interface conversacional | Magie passou de 1 mi de usuários; vários assistentes via WhatsApp | [S] |
| Forecasting como tier premium | Monarch Plus (abr/2026) cobra o dobro do plano base por projeções e what-ifs | [P]/[S] |

Leitura para o Wall.ia: "IA agêntica" que **age** sobre o dinheiro vai chegar primeiro pelos bancos, que têm licença e dados. O posicionamento "IA read-only que só explica números calculados" vai na direção contrária e combina com a fase de governança e rastreabilidade do Open Finance. Vira argumento de confiança, não de atraso.

---

## Números-chave

| Métrica | Valor | Data | Fonte URL |
|---|---|---|---|
| Consentimentos ativos Open Finance | 208,79 mi | 31/07/2026 | https://www.letsmoney.com.br/noticias/open-finance-208-milhoes-consentimentos-governanca/ |
| PFs únicas na recepção OF | 138,46 mi | 13/08/2026 | https://www.letsmoney.com.br/noticias/open-finance-208-milhoes-consentimentos-governanca/ |
| Clientes/contas conectados OF | >100 mi | fev/2026 | https://finsidersbrasil.com.br/economia-open/brasil-lidera-open-finance-no-mundo-com-100-milhoes-de-clientes/ |
| Usuários PF Pix | 148,3 mi (2025); ~170 mi (2026) | 2025/2026 | https://www.bcb.gov.br/content/estabilidadefinanceira/pix/relatorio_de_gestao_pix/relatorio_gestao_pix_2026.pdf |
| Adultos com conta | 96,4% | abr/2026 | https://www.poder360.com.br/poder-economia/bancarizacao-atinge-964-da-populacao-brasileira-diz-bc/ |
| Transações via celular | 78% de 240,8 bi | 2025 | https://portal.febraban.org.br/noticia/4469/pt-br/ |
| Famílias endividadas (Peic) | 82,0% | ago/2026 | https://portaldocomercio.org.br/acoes-institucionais/cnc-mais-endividadas-do-que-a-media-familias-de-menor-renda-lideram-avanco-na-inadimplencia-em-agosto/ |
| Famílias com contas em atraso | 29,9% | ago/2026 | idem |
| Cartão entre as dívidas das famílias | 84,6% | mai/2026 | https://static.poder360.com.br/2026/06/endividamento-cnc-peic-maio2026.pdf |
| Inadimplentes Serasa | 83,9 mi (51% dos adultos) | ago/2026 | https://mais.opovo.com.br/jornal/farol/2026/09/19/com-8398-milhoes-negativados-inadimplencia-no-br-alcanca-menor-patamar-de-crescimento-do-ano-aponta-serasa.html |
| Comprometimento de renda (BCB) | 28,85% | jun/2026 | https://api.bcb.gov.br/dados/serie/bcdata.sgs.29034/dados?formato=json |
| Endividamento/renda 12m (BCB) | 49,75% | jun/2026 | https://api.bcb.gov.br/dados/serie/bcdata.sgs.29037/dados?formato=json |
| Juros rotativo cartão PF | 436,15% a.a. | jul/2026 | https://api.bcb.gov.br/dados/serie/bcdata.sgs.22022/dados?formato=json |
| Selic meta | 13,75% a.a. | set/2026 | https://api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados/ultimos/1?formato=json |
| Volume cartões | R$ 4,5 tri | 2025 | https://panoramaabecs.com.br/economia-pagamentos-cartoes-brasil-2025-dados-abecs/ |
| Parcelado sem juros no crédito | 42,6% | 2025 | https://panoramaabecs.com.br/economia-pagamentos-cartoes-brasil-2025-dados-abecs/ |
| Mobills Premium | R$ 99,90/ano; >10 mi usuários | 2026 | https://www.mobills.com.br/pricing/ |
| Organizze | R$ 35–69/mês | 2026 | https://www.organizze.com.br/planos/ |
| Guiabolso: usuários / preço de compra | 6 mi / R$ 150–200 mi | 2021 | https://exame.com/invest/minhas-financas/picpay-compra-guiabolso-de-olho-no-open-banking/ |
| Monarch Plus | US$ 199/ano | abr/2026 | https://help.monarch.com/hc/en-us/articles/48344305092244-Forecasting-in-Monarch |
| Monarch valuation | US$ 850 mi | mai/2025 | https://techfundingnews.com/personal-finance-app-monarch-rises-with-75m-after-mints-meltdown-a-soonicorn-on-cards/ |
| Cleo ARR | US$ 280 mi | jul/2025 | https://sacra.com/c/cleo/ |
| Conversão freemium D35 | 2,1% | 2025/2026 | https://www.revenuecat.com/blog/growth/subscription-app-trends-benchmarks-2026 |
| Pluggy Dados (piso) | R$ 2.500/mês | 2026 | https://www.pluggy.ai/precos |
| Pix Automático | R$ 1,2 bi/mês; R$ 4,5 bi acumulado 2026 | mai/2026 | https://finsidersbrasil.com.br/pagamentos/pix/um-ano-depois-pix-automatico-ainda-nao-pegou-e-ha-um-motivo |
| Investimento bancos em TI | R$ 50,4 bi (R$ 3 bi GenAI) | 2026 | https://brasilinovador.com.br/investimento-dos-bancos-em-ti-deve-atingir-r-504-bilhoes-no-brasil-em-2026/ |
| Portabilidade de crédito via OF | operando desde 02/02/2026 | 2026 | https://www.poder360.com.br/poder-economia/portabilidade-de-credito-via-open-finance-entra-em-operacao-no-pais/ |

---

## Cenários de projeção, 36 meses (PFM freemium B2C no Brasil)

### Modelo

`pagantes(t) = cadastros_acumulados(t) × %ativos × %pagantes_dos_ativos`
`MRR(t) = pagantes(t) × ARPPU_líquido`
Receita acumulada em 36 meses, pela regra do trapézio sobre M0, M12, M24 e M36: `12×MRR12 + 12×MRR24 + 6×MRR36`.

### Premissas e origem

| Premissa | Conservador | Base | Otimista | De onde vem |
|---|---|---|---|---|
| Cadastros acumulados em M36 | 60 mil | 250 mil | 900 mil | Comparáveis: Financinha 38 mil (2026), ZapGastos 17 mil ativos, Magie 400 mil em ~18 meses com >US$ 10 mi captados, Guiabolso 6 mi e Mobills 10 mi em ~7–8 anos. O otimista pressupõe captação ou canal de distribuição (B2B2C). **Premissa própria calibrada por comparáveis.** |
| Curva de cadastros (% do total em M12/M24/M36) | 15/45/100 | 15/45/100 | 15/45/100 | Premissa própria (crescimento acelerando depois do product-market fit) |
| % de cadastros ativos no mês | 30% | 30% | 35% | **Não encontrado dado público** de MAU/cadastros em PFM brasileiro. Premissa própria. |
| % de pagantes entre ativos | 3% | 5% | 7% | Ancorado nos 2,1% de download para pago no D35 do freemium (RevenueCat). Sobre a base **ativa** a taxa tende a ser maior. O otimista supõe paywall no Modo Futuro com trial longo (RevenueCat: trial de 17–32 dias converte 42,5%). |
| Preço de tabela | R$ 14,90/mês ou R$ 119,90/ano | idem | idem, mais um tier de R$ 24,90 | Entre Mobills (R$ 8,33) e Organizze (R$ 33–69); tier superior espelha o Monarch Plus |
| ARPPU líquido | R$ 8,50 | R$ 8,50 | R$ 10,00 | Mix anual/mensal (~R$ 10–11) menos ~15% de taxa da loja (Apple Small Business Program e Google: 15% para receita até US$ 1 mi/ano). O otimista inclui o tier superior. |
| Custo do agregador OF | R$ 2.500/mês de piso | idem, mais excedente (não publicado) | idem, com contrato negociado | Pluggy, preço público |

### Resultado

| Cenário | Marco | Cadastros acum. | Ativos | Pagantes | MRR líquido | ARR (run-rate) |
|---|---|---|---|---|---|---|
| Conservador | M12 | 9.000 | 2.700 | 81 | R$ 689 | R$ 8,3 mil |
| | M24 | 27.000 | 8.100 | 243 | R$ 2.066 | R$ 24,8 mil |
| | M36 | 60.000 | 18.000 | 540 | R$ 4.590 | R$ 55,1 mil |
| Base | M12 | 37.500 | 11.250 | 563 | R$ 4.781 | R$ 57,4 mil |
| | M24 | 112.500 | 33.750 | 1.688 | R$ 14.344 | R$ 172,1 mil |
| | M36 | 250.000 | 75.000 | 3.750 | R$ 31.875 | R$ 382,5 mil |
| Otimista | M12 | 135.000 | 47.250 | 3.308 | R$ 33.075 | R$ 396,9 mil |
| | M24 | 405.000 | 141.750 | 9.923 | R$ 99.225 | R$ 1,19 mi |
| | M36 | 900.000 | 315.000 | 22.050 | R$ 220.500 | R$ 2,65 mi |

| Cenário | Receita líquida acumulada em 36 meses | Custo mínimo do agregador em 36 meses (R$ 2.500 × 36) | Receita menos agregador |
|---|---|---|---|
| Conservador | ~R$ 60,6 mil | R$ 90 mil | **negativo (-R$ 29 mil)** antes de pessoal, infra e marketing |
| Base | ~R$ 420,8 mil | R$ 90 mil + excedente | ~R$ 330 mil antes de pessoal, infra e marketing |
| Otimista | ~R$ 2,91 mi | R$ 90 mil + excedente relevante | ~R$ 2,8 mi antes de pessoal, infra, marketing e custo de LLM |

### Teto de CAC (onde o modelo quebra)

- LTV por pagante ≈ ARPPU líquido × vida média paga. A RevenueCat indica que a maioria dos assinantes anuais cancela em até 1 ano. Supondo **12 meses de vida média** (premissa), o LTV fica em ~R$ 102 (base).
- LTV por **cadastro** = R$ 102 × 30% × 5% ≈ **R$ 1,53** (base); ≈ R$ 0,92 (conservador); ≈ R$ 2,45 (otimista).
- CAC pago de apps de finanças no Brasil: **não encontrado**. Qualquer CAC pago acima de R$ 1–2,50 por cadastro destrói valor nesse modelo. **Conclusão:** B2C por mídia paga não fecha. O crescimento precisa vir de canal orgânico (conteúdo, indicação, WhatsApp) ou de **B2B2C/white-label** (cooperativas, fintechs sem PFM, benefícios corporativos). Foi o caminho que a Magie tomou em 2026.

### Sensibilidade (o que mais move o resultado)

1. **% de pagantes entre ativos:** cada ponto percentual no cenário base vale ~R$ 76,5 mil de ARR em M36.
2. **Custo por conexão OF para usuários gratuitos:** não publicado. Se o excedente for cobrado por conta ativa, o plano grátis com Open Finance pode custar mais que a receita. Mitigação: plano grátis com 1 conexão, ou dados manuais e sandbox.
3. **Retenção:** se a vida média paga cair de 12 para 6 meses, o LTV cai pela metade.

### O que ficou de fora

- Custos de pessoal, infraestrutura, LLM e compliance (não estimados).
- Receita B2B2C e afiliação de crédito (excluída de propósito, porque conflita com o pilar "confiança").
- Números do dashboard OFB, do Pix Automático e o dado de retenção anual da RevenueCat vieram de fonte secundária ou de resumo. Conferir na fonte primária antes de citar em banca ou pitch.
