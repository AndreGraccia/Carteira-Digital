# ADR 0001: Motor Futuro determinístico, IA só explica

Status: aceito, 22/09/2026.

## Contexto

O Wall.ia quer mostrar o que já tem destino no saldo do usuário e permitir testar decisões antes de tomá-las ("e se eu comprar um celular de R$ 3.000 em 10 vezes?"). Esses números vão orientar decisões de dinheiro de verdade. Um valor errado aqui custa mais do que qualquer tela bonita ganha.

Havia duas formas de calcular:

1. Mandar o extrato para um LLM (Gemini/OpenAI) e pedir a projeção.
2. Calcular no backend, em Java, com regras fixas, e usar o LLM apenas para explicar o resultado.

## Decisão

Opção 2. A regra do produto é: Java calcula, a IA explica, o usuário decide.

- O cálculo mora em `backend/.../service/future/`, em classes sem Spring (`RecurrenceDetector`, `CashEventPlanner`, `ProjectionEngine`, `ScenarioSimulator`), testáveis com JUnit puro.
- Uma recorrência detectada nunca entra na projeção sozinha. Ela vira pergunta ("Esse valor que você recebe da Concentrix S.A. é seu salário?") e só conta depois que o usuário confirma (`RecurrenceDecision`, migração V11).
- Todo evento projetado carrega origem (instituição, conta), referência e se é `CONFIRMED` (informado pela instituição) ou `ESTIMATED` (padrão confirmado pelo usuário). A tela nunca soma os dois sem dizer quanto é cada um.
- O Modo Futuro (`POST /api/v1/future/scenarios`) roda o mesmo motor sobre uma cópia dos eventos. Nada é gravado.

## Consequências

Ganhamos:

- Resultado reproduzível. O caso do demo (saldo mínimo de -R$ 324,87 entre 03/10 e 04/10, fechado pelo salário do dia 5) está travado em teste (`ContractSandboxOpenFinanceGatewayTest`).
- Nenhum dado de extrato sai do servidor para calcular projeção.
- Custo de LLM zero no caminho principal.

Perdemos:

- Categorização mais "esperta" por texto livre. O detector só reconhece padrões de valor, dia e contraparte; compras variáveis (mercado, transporte) ficam fora da projeção e a tela diz isso.
- Cada novo tipo de compromisso (Pix Automático, consórcio) exige código e teste, não um prompt.

## Como reverter

O motor é isolado atrás de `FutureService` e do controller `/api/v1/future`. Trocar o cálculo não mexe em carteiras, transações nem Open Finance. A migração V11 só adiciona tabelas (`credit_card`, `card_charge`, `loan`, `recurrence_decision`); um rollback é remover as tabelas e o pacote `service/future`.
