# ADR 0017: Spring Boot 4.1 e Java 25

Status: aceito e implementado. 25/09/2026.

## Contexto

A linha 3.5 do Spring Boot ficou sem suporte gratuito em 30/06/2026. Sem correção de segurança pública, cada CVE novo em Spring, Tomcat, Jackson ou Hibernate ficaria aberto. O projeto rodava Boot 3.5.16 em Java 17. O Java 25 é a LTS de 16/09/2025.

## Decisão

Migrar em dois passos, cada um com a suíte inteira verde antes do próximo, numa branch separada (`feat/boot4`):

1. **Boot 4.1.1, ainda em Java 17.**
   - **Jackson 3.** `ObjectMapper` e `JsonNode` vêm de `tools.jackson.databind`; as anotações continuam em `com.fasterxml.jackson.annotation`. O customizador passou a ser `JsonMapperBuilderCustomizer`. Não foi usado o módulo de compatibilidade com Jackson 2, que já nasce depreciado.
   - **Spring Security 7.** A configuração usa só o estilo com lambdas (o `.and()` saiu), e o `DaoAuthenticationProvider` recebe o `UserDetailsService` no construtor.
   - **Starters modulares.** O Flyway passou a ser configurado pelo `spring-boot-starter-flyway`; só o `flyway-core` já não roda as migrações. Os testes de MVC e de métricas vieram de `spring-boot-starter-webmvc-test` e `spring-boot-starter-micrometer-metrics-test`, com as anotações em pacotes novos.
   - **springdoc 3.1.1**, Lombok 1.18.48 e MapStruct 1.6.3.
2. **Java 25.** `<release>25`, imagens `maven:3.9-eclipse-temurin-25` e `eclipse-temurin:25-jre-jammy`, CI com Java 25, e JaCoCo 0.8.15, porque a 0.8.12 não instrumenta classes do Java 25.

## Como foi verificado

- **Boot 4 em Java 17:** 334 testes, 0 falhas, 0 pulados, contra PostgreSQL 14.6.
- **Java 25:** a mesma suíte num container com JDK 25. As classes saem com major version 69.
- **Imagem nova na pilha de homologação:** health check `healthy` e o E2E no navegador passou inteiro. Ele confere o que os testes do backend não veem: se o JSON do Jackson 3 continua no formato que o frontend lê.
- **springdoc ligado:** OpenAPI 3.1 com as 47 rotas e o Swagger UI respondendo. Desligado em produção, como antes.

## Consequências

- Volta a haver correção de segurança gratuita, e a linha 4.x tem suporte até 2027 ou mais.
- **Quem desenvolve precisa do JDK 25.** Com um JDK mais antigo, `mvnw` não compila. Sem JDK 25, o guia (seção 14) mostra como testar pelo Docker.
- O `jjwt` 0.12.6 continua usando Jackson 2 por dentro (`jjwt-jackson`). Isso não conflita, porque os pacotes são diferentes, mas são duas versões de Jackson no classpath. Trocar quando o `jjwt` publicar suporte a Jackson 3.
- **Como voltar atrás:** reverter os dois commits da migração. Nenhuma migração de banco foi criada, então o banco não precisa de volta.
