package com.github.yildizmy.dto.request;

/** @param months how long the data may be shared: 3, 6 or 12; null means 12. */
public record OpenFinanceConsentRequest(Integer months) {
}
