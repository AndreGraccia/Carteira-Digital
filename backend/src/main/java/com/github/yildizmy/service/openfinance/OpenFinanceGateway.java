package com.github.yildizmy.service.openfinance;

public interface OpenFinanceGateway {
    OpenFinanceSnapshot fetch(long userId);
    String mode();
    boolean simulated();

    /** The transmitting institution the consent is created with, known before any data is read. */
    String institutionId();

    String institutionName();
}
