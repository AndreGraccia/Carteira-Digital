# ADR 0007: dia útil bancário e datas de borda no motor Futuro

Status: aceito e implementado. 24/09/2026.

## Contexto

A auditoria de 24/09 (`docs/auditoria-2026-09-24/backend.md`, F1 a F5) achou cinco casos em que o motor mostrava data ou valor errado sem nenhum teste falhar:

1. Salário do dia 30 pago no dia 1 (fim de semana) fazia o mês seguinte contar como "já recebido". A projeção perdia um salário inteiro e disparava alerta de negativo falso.
2. Vencimentos e rendas caíam em sábado, domingo e feriado. O buraco era apontado com 1 a 3 dias de erro, justamente no dia que importa.
3. Empréstimo com vencimento no dia 31 informado como 28/02 passava a vencer todo dia 28.
4. "Juros que faltam" usava a fórmula da tabela Price também para SAC (R$ 1.000 onde o certo é R$ 550).
5. Com a renda caindo amanhã, o limite diário virava o saldo inteiro.

## Decisão

**Calendário bancário nacional (`BusinessDays`).** Fim de semana, os 9 feriados nacionais fixos (inclusive Consciência Negra, Lei 14.759/2023) e os dias móveis em que os bancos fecham: segunda e terça de Carnaval, Sexta-feira Santa e Corpus Christi, calculados a partir da Páscoa.

| Evento | Dia fechado vira |
| --- | --- |
| Conta, fatura, parcela | próximo dia útil (é o que o banco aceita sem multa) |
| Salário e outras entradas | dia útil anterior (prática comum de folha) |
| Meta de guardar | não muda (é o próprio usuário quem move) |

**Recorrência por distância, não por mês.** Uma ocorrência prevista a menos de 20 dias da última vista é considerada a mesma. O dia típico é calculado de forma circular: 30, 30 e 1 viram dia 30, não dia 1.

**Dia contratual do empréstimo (V23).** Nova coluna `loan.due_day`. Se a instituição manda o dia, ele vale. Se só manda a próxima data e ela é o último dia de um mês curto, o dia já conhecido é mantido.

**Sistema de amortização (V23).** Nova coluna `loan.amortization_system`, o mesmo nome do campo `amortizationScheduled` da API Loans do Open Finance. Price: parcela × restantes − saldo. SAC: taxa mensal × saldo × (n + 1) / 2. Qualquer outro sistema: nenhum número, em vez de um número errado.

**Limite com renda amanhã.** Uma renda que cai amanhã não deixa nenhum dia de gasto antes dela. O limite passa a valer até a renda seguinte (ou 30 dias).

## Consequências

- A conta de demonstração do sandbox mudou o vencimento do cartão de 3 para 2. Em outubro de 2026 o dia 3 é sábado; a fatura iria para o dia 5, junto com o salário, e o caso que a demonstração quer mostrar (um buraco antes do salário) sumiria.
- Feriados estaduais e municipais não entram. Se a projeção precisar deles, a região do usuário vira um dado de perfil.
- Os testes de datas de borda ficam em `CashEventPlannerTest` e `BusinessDaysTest`.

## Descartado

- **Biblioteca de feriados.** Resolveria os feriados locais, mas acrescenta uma dependência para um cálculo de 20 linhas; a máquina de build também não baixa pacotes novos com facilidade.
- **Ajustar dentro de `CardPlan.dueDate`.** O cartão usa a data nominal para decidir em qual fatura cada compra entra. O ajuste só vale para o dia do pagamento.
