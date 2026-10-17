# ADR 0004: trilha de auditoria administrativa agora, ledger de dupla entrada quando houver dinheiro em movimento

Status: auditoria aceita e implementada; ledger proposto. 23/09/2026.

## Contexto

Os dois últimos itens da estratégia eram "papéis administrativos com trilha de auditoria" e "ledger de dupla entrada". Antes de construir, conferi no código o que cada um protegeria hoje.

**Administração.** Quatro endpoints exigem `ROLE_ADMIN`: listar todas as carteiras, listar todas as transações, métricas do Pergunte e disparar o job de alertas. Os dois primeiros mostram dados financeiros de todos os usuários, e nada registrava quem os consultou.

**Movimentação de dinheiro.** Uma carteira local nasce com saldo zero (`WalletService.create`). Não existe depósito desde a remoção da "adição de dinheiro". Contas do Open Finance são somente leitura (`OPEN_FINANCE_READ_ONLY`). Resultado: transferência e retirada só existem no código. Na prática, nenhum real se move dentro do Wall.ia. O saldo que importa vem da instituição, pelo Open Finance.

## Decisão

**Trilha de auditoria administrativa, agora.**

- `AdminAuditInterceptor` grava em `admin_audit` (V18) toda chamada a endpoint protegido por `hasRole('ADMIN')`: quem, método, caminho, status e quando. Tentativas negadas também entram, porque um usuário comum tentando `/wallets` é justamente o que se quer ver.
- Não guarda corpo nem parâmetros de consulta, que podem ter dado pessoal.
- `GET /api/v1/admin/audit?days=7` mostra a trilha só para administradores, e essa leitura também é auditada.
- Ficam para depois papéis novos (suporte, operações, auditoria): nenhuma funcionalidade usa esses papéis hoje. Criar papel sem permissão associada é configuração sem efeito.

**Ledger de dupla entrada, proposto.**

Quando o gatilho acontecer, o desenho é este:

| Peça | Como |
| --- | --- |
| `ledger_account` | Uma conta contábil por carteira, mais contas técnicas (entrada Open Finance, saída para terceiros, ajustes) |
| `ledger_entry` | Somente inserção. Cada movimento gera lançamentos cuja soma é zero, na mesma transação do banco |
| Saldo | Derivado da soma dos lançamentos; `wallet.balance` vira cache conferido |
| Correção | Por estorno (lançamento inverso), nunca por `UPDATE` ou `DELETE` |
| Conciliação | Job diário comparando a soma dos lançamentos com o saldo de cada carteira; divergência gera alerta interno |
| Migração | Lançamento de abertura por carteira, igual ao saldo atual, marcado como ajuste inicial |

**Gatilho:** a primeira funcionalidade que coloque dinheiro numa carteira local ou tire dela para fora. Por exemplo, iniciação de pagamento Pix pelo Open Finance, ou a volta do depósito.

## Alternativas consideradas

| Alternativa | A favor | Contra |
| --- | --- | --- |
| Construir o ledger agora | Base pronta | Contabilizaria um fluxo que não existe; semanas de trabalho sem nenhum lançamento real para testar |
| Ledger "sombra" gravando só transferências | Pequeno | Pelo mesmo motivo, ficaria vazio |
| Auditoria por log de aplicação | Zero migração | Log gira e some (a política atual guarda 10 MB); auditoria precisa de retenção e consulta |
| Auditoria de todas as requisições | Cobertura total | Volume grande e dado pessoal em rotas comuns; o risco concentrado está nas rotas de admin |

## Consequências

- Acesso administrativo a dados de todos os usuários fica rastreável, com a identidade de quem acessou.
- O interceptor depende da anotação `@PreAuthorize("hasRole('ADMIN')")`. Um endpoint administrativo novo que use outra forma de proteção não será auditado. Regra do projeto: endpoint de admin usa essa anotação.
- A tela "Movimentar" hoje não tem como ser usada com saldo real: carteiras locais nunca recebem dinheiro. Decidir entre dar a ela um propósito (por exemplo, "separar dinheiro" em envelopes simulados) ou retirá-la é uma decisão de produto, registrada na estratégia.

## Como reverter

- Remover `WebConfig` desliga a auditoria sem afetar mais nada. A V18 só cria uma tabela.
- O ledger não foi implementado; não há o que reverter.
