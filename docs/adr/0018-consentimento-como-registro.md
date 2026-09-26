# ADR 0018: consentimento do Open Finance como registro próprio

Status: aceito e implementado. 26/09/2026. Item B42 do backlog.

## Contexto

Até a V31, o consentimento morava na linha de `open_finance_connection`, que é única por usuário e instituição. Cada novo consentimento sobrescrevia o anterior: id, datas, status e motivo da rejeição. Quem conectou, revogou e conectou de novo tinha só o último registro. As permissões ficavam num texto separado por vírgula, e não havia como saber qual texto a pessoa leu ao autorizar.

A LGPD (art. 8º, § 2º) põe no controlador o ônus de provar que o consentimento foi obtido, e o consentimento precisa ter finalidade determinada. Com a linha sobrescrita, a Wall.ia não consegue provar nada sobre um consentimento que já acabou.

## Decisão

1. **Um registro por consentimento, nunca sobrescrito** (`open_finance_consent`): usuário, instituição, finalidade (`FUTURE_PROJECTION`), criação, validade e, ao autorizar, a versão e o idioma do texto lido.
2. **Permissões numa tabela** (`open_finance_consent_permission`), uma linha por permissão.
3. **Histórico só de acréscimo** (`open_finance_consent_event`): `CREATED`, `AUTHORISED` e `REJECTED` com quem rejeitou e por quê. O banco exige o motivo em toda rejeição.
4. **A conexão continua existindo** e guarda só o estado atual, que é o que o sync e a tela leem. Todo caminho que encerra um consentimento (recusa, revogação, prazo, 60 minutos sem autorizar, revogação no banco) passa por um único método, que grava o evento na mesma transação da mudança de estado.
5. **Versão do texto.** O frontend manda `termsVersion` e `language` ao autorizar. O backend só aceita versões conhecidas (`OpenFinanceConsentRecord.TERMS_VERSIONS`); sem elas, responde 422 `OPEN_FINANCE_TERMS_UNKNOWN` e nada é autorizado. O texto é identificado por um hash em `frontend/src/consentTerms.json`: se alguém muda o texto sem publicar versão nova, o `test:i18n` falha no CI.
6. **Migração.** A V32 copia o que existe (um consentimento por conexão; os anteriores já tinham sido perdidos) e deixa a coluna `permissions` da conexão sem uso e anulável. Apagá-la é outra migração, com aprovação do dono.
7. **Titular.** A exportação traz os consentimentos, com permissões, e os eventos. A exclusão de conta apaga os registros junto com o resto do Open Finance, como já decidido no ADR 0012.

## Consequências

- Uma versão nova do texto pede duas mudanças no mesmo commit: a versão e o hash no `consentTerms.json` e a versão em `TERMS_VERSIONS`. Versões antigas ficam aceitas, porque uma aba aberta antes do deploy ainda mostra o texto antigo, e é isso que a pessoa leu.
- O registro não depende do provedor. Quando o agregador for escolhido (B40), o id do consentimento dele entra no mesmo lugar.
- **Pergunta para o jurídico:** apagar a prova do consentimento na exclusão da conta está de acordo com o ADR 0012, mas a LGPD (art. 16, II e art. 7º, VI) pode justificar guardá-la por um prazo, para defesa em processo. Se o jurídico pedir, basta tirar a linha do `AccountDeletionService` e definir o prazo no ADR 0015.

## Descartado

- **Tabela nova só com eventos, mantendo a conexão como registro.** A finalidade, a validade e o texto do consentimento antigo continuariam sendo sobrescritos.
- **Permissões num array do PostgreSQL.** Funciona, mas o mapeamento e a consulta ficam mais obscuros que uma tabela de duas colunas, e o backlog pedia tabela.
- **Versão do texto calculada no navegador (hash em tempo de execução).** Exigiria `crypto.subtle` assíncrono na tela e ainda assim o backend precisaria de uma lista. O hash conferido no CI dá a mesma garantia sem código na tela.
