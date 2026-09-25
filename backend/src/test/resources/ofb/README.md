# Fixtures do Open Finance Brasil

Uma resposta por endpoint, montada a partir dos schemas oficiais com o `example` que cada campo traz na especificação. Os parsers do `ParticipantSandboxOpenFinanceGateway` são testados contra elas (`OfbResponsesTest`): se um nome de campo do código divergir do padrão, o teste quebra.

| API | Versão | Arquivo de origem |
| --- | --- | --- |
| Accounts | 2.4.2 | `swagger-accounts.yaml` |
| Credit Cards Accounts | 2.3.1 | `swagger-credit-cards-accounts.yaml` |
| Loans | 2.5.0 | `swagger-loans.yaml` |
| Resources | 3.1.0 | `swagger-resources.yaml` |

Fonte: repositório oficial `OpenBanking-Brasil/openapi` (último commit consultado em 19/12/2025), licença Apache 2.0.

Para regerar depois de baixar os YAML numa pasta (nomes `accounts.yml`, `cards.yml`, `loans.yml`, `resources.yml`, ao lado do script):

```
python generate.py caminho/para/esta/pasta
```
