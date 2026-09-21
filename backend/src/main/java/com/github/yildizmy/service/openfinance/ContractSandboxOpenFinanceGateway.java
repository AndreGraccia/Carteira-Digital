package com.github.yildizmy.service.openfinance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.open-finance.mode", havingValue = "contract-sandbox", matchIfMissing = true)
public class ContractSandboxOpenFinanceGateway implements OpenFinanceGateway {

    private final Clock clock;

    public ContractSandboxOpenFinanceGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OpenFinanceSnapshot fetch(long userId) {
        Instant now = clock.instant();
        var checkingEntries = List.of(
                entry("salary-01", "CREDITO", "Salário", "SALARIO", "5200.00", now.minus(5, ChronoUnit.DAYS)),
                entry("market-01", "DEBITO", "Mercado Bairro", "COMPRA_DEBITO", "486.72", now.minus(7, ChronoUnit.DAYS)),
                entry("rent-01", "DEBITO", "Aluguel residencial", "PIX", "1450.00", now.minus(11, ChronoUnit.DAYS)),
                entry("coffee-03", "DEBITO", "Cafeteria Centro", "COMPRA_DEBITO", "22.90", now.minus(12, ChronoUnit.DAYS)),
                entry("stream-01", "DEBITO", "Assinatura streaming", "DEBITO_AUTOMATICO", "39.90", now.minus(16, ChronoUnit.DAYS)),
                entry("transport-02", "DEBITO", "Mobilidade urbana", "PIX", "34.50", now.minus(18, ChronoUnit.DAYS)),
                entry("restaurant-02", "DEBITO", "Restaurante", "COMPRA_DEBITO", "96.40", now.minus(22, ChronoUnit.DAYS)),
                entry("salary-00", "CREDITO", "Salário", "SALARIO", "5200.00", now.minus(35, ChronoUnit.DAYS)),
                entry("market-00", "DEBITO", "Mercado Bairro", "COMPRA_DEBITO", "514.28", now.minus(39, ChronoUnit.DAYS)),
                entry("rent-00", "DEBITO", "Aluguel residencial", "PIX", "1450.00", now.minus(42, ChronoUnit.DAYS)),
                entry("coffee-02", "DEBITO", "Cafeteria Centro", "COMPRA_DEBITO", "19.80", now.minus(45, ChronoUnit.DAYS)),
                entry("stream-00", "DEBITO", "Assinatura streaming", "DEBITO_AUTOMATICO", "39.90", now.minus(47, ChronoUnit.DAYS)),
                entry("course-00", "DEBITO", "Curso online", "PIX", "149.00", now.minus(54, ChronoUnit.DAYS))
        );
        var savingsEntries = List.of(
                entry("reserve-02", "CREDITO", "Transferência para reserva", "TRANSFERENCIA_MESMA_INSTITUICAO", "350.00", now.minus(8, ChronoUnit.DAYS)),
                entry("reserve-01", "CREDITO", "Transferência para reserva", "TRANSFERENCIA_MESMA_INSTITUICAO", "300.00", now.minus(38, ChronoUnit.DAYS))
        );
        return new OpenFinanceSnapshot(
                "sandbox-21128159000166",
                "Banco Aurora Sandbox",
                UUID.nameUUIDFromBytes(("ofb-consent-" + userId).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),
                now,
                now.plus(90, ChronoUnit.DAYS),
                List.of(
                        new OpenFinanceSnapshot.Account(
                                "acc-checking-demo", "Banco Aurora Sandbox", "CONTA_DEPOSITO_A_VISTA",
                                "0001", "94088392", "4", new BigDecimal("5842.70"), checkingEntries),
                        new OpenFinanceSnapshot.Account(
                                "acc-savings-demo", "Banco Aurora Sandbox", "CONTA_POUPANCA",
                                "0001", "24550245", "7", new BigDecimal("2650.00"), savingsEntries)
                )
        );
    }

    private OpenFinanceSnapshot.Entry entry(
            String id, String direction, String name, String type, String amount, Instant date) {
        return new OpenFinanceSnapshot.Entry(id, direction, name, type, new BigDecimal(amount), date);
    }

    @Override
    public String mode() {
        return "CONTRACT_SANDBOX";
    }

    @Override
    public boolean simulated() {
        return true;
    }
}
