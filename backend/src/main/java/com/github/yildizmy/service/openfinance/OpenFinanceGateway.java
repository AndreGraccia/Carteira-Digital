package com.github.yildizmy.service.openfinance;

public interface OpenFinanceGateway {
    OpenFinanceSnapshot fetch(long userId);
    String mode();
    boolean simulated();
}
