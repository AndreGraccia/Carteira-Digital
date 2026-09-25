package com.github.yildizmy.service.openfinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test: the parsers read responses built from the official Open Finance Brasil schemas
 * (src/test/resources/ofb). A field renamed in the code, or read under a name the standard does not use, breaks here.
 */
class OfbResponsesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2021, 5, 10);

    @Test
    void accountTransactionKeepsTheOfficialFieldsIncludingTheScheduledFlag() throws IOException {
        JsonNode transaction = data("OKResponseAccountTransactions").get(0);

        var entry = OfbResponses.entry(transaction);

        assertEquals("TXpRMU9UQTROMWhZV2xSU1FUazJSMDl", entry.transactionId());
        assertEquals("DEBITO", entry.creditDebitType());
        assertEquals("PIX", entry.type());
        assertEquals(new BigDecimal("1000.04"), entry.amount());
        assertEquals("TRANSACAO_EFETIVADA", entry.completedAuthorisedPaymentType());

        ((ObjectNode) transaction).put("completedAuthorisedPaymentType", "LANCAMENTO_FUTURO");
        assertTrue(OfbResponses.entry(transaction).scheduled());
    }

    @Test
    void balanceIsTheAvailableAmount() throws IOException {
        assertEquals(new BigDecimal("1000.04"), OfbResponses.availableBalance(data("OKResponseAccountBalances")));
    }

    @Test
    void resourcesListOnlyWhatIsAvailable() throws IOException {
        JsonNode resources = data("OKResponseResourceList");

        assertEquals(Set.of("25cac914-d8ae-6789-b215-650a6215820d"), OfbResponses.available(resources, "ACCOUNT"));
        ((ObjectNode) resources.get(0)).put("status", "TEMPORARILY_UNAVAILABLE");
        assertTrue(OfbResponses.available(resources, "ACCOUNT").isEmpty());
    }

    @Test
    void cardReadsLimitNextBillWithMinimumAndFoldsInstalmentsIntoOnePurchase() throws IOException {
        JsonNode account = data("OKResponseCreditCardAccountsList").get(0);
        JsonNode bill = data("OKResponseCreditCardAccountsBills").get(0);
        ObjectNode second = purchase(bill.path("billId").asText(), 2);
        ObjectNode third = purchase("bill-june", 3);
        ObjectNode refund = purchase(bill.path("billId").asText(), 2);
        refund.put("transactionType", "ESTORNO");

        var card = OfbResponses.card(account, data("OKResponseCreditCardAccountsLimits"),
                data("OKResponseCreditCardAccountsBills"), List.of(second, third, refund), TODAY);

        assertEquals("XXZTR3459087", card.creditCardAccountId());
        assertEquals("Cartão Universitário", card.name());
        assertEquals(new BigDecimal("1000.04"), card.limitAmount());
        assertEquals("4453", card.lastDigits());
        assertEquals(21, card.dueDay());
        assertEquals(11, card.closingDay(), "estimated 10 days before the due date");
        assertEquals(new BigDecimal("1000.04"), card.nextBill().minimumAmount());
        assertEquals(1, card.transactions().size(), "instalment 2 and 3 of the same purchase are one charge; refunds are not purchases");
        var charge = card.transactions().get(0);
        assertEquals(3, charge.installments());
        assertEquals(new BigDecimal("3000.12"), charge.amount());
        assertEquals(LocalDate.of(2021, 4, 21), charge.firstInvoiceDue(), "bill of 21/05 carries instalment 2, so 1 was in April");
    }

    @Test
    void monthlyLoanWithRateCetAndOutstandingBalance() throws IOException {
        ObjectNode contract = (ObjectNode) data("OKResponseLoansContract");
        contract.put("instalmentPeriodicity", "MENSAL");
        contract.put("firstInstalmentDueDate", "2020-01-31");
        ObjectNode instalments = (ObjectNode) data("OKResponseLoansInstalments");
        instalments.put("totalNumberOfInstalments", 24);
        instalments.put("paidInstalments", 13);

        var loan = OfbResponses.loan("contract-1", contract, instalments, data("OKResponseLoansPayments"), TODAY);

        assertEquals("Crédito Pessoal Consignado", loan.productName());
        assertEquals(new BigDecimal("1000.04"), loan.outstandingBalance());
        assertEquals(24, loan.totalInstallments());
        assertEquals(13, loan.paidInstallments());
        assertEquals(LocalDate.of(2021, 2, 28), loan.nextDueDate(), "14th instalment, day 31 in a short month");
        assertEquals(31, loan.dueDay());
        assertEquals("SAC", loan.amortizationSystem());
        assertEquals(new BigDecimal("0.290000"), loan.cet());
        assertEquals(new BigDecimal("60.0000"), loan.annualRate(), "0.6 per year (AA) is 60% a.a.");
    }

    @Test
    void aWeeklyLoanIsLeftOutInsteadOfGuessed() throws IOException {
        assertNull(OfbResponses.loan("c", data("OKResponseLoansContract"), data("OKResponseLoansInstalments"),
                data("OKResponseLoansPayments"), TODAY), "the official example is SEMANAL");
    }

    @Test
    void monthlyRateBecomesAnnual() {
        ObjectNode rate = JSON.createObjectNode().put("taxPeriodicity", "AM").put("preFixedRate", "0.02");

        assertEquals(new BigDecimal("26.8242"), OfbResponses.annualRatePercent(JSON.createArrayNode().add(rate)));
    }

    private ObjectNode purchase(String billId, int instalment) throws IOException {
        ObjectNode transaction = (ObjectNode) data("OKResponseCreditCardAccountsTransactions").get(0).deepCopy();
        transaction.put("transactionType", "OUTROS");
        transaction.put("paymentType", "A_PRAZO");
        transaction.put("chargeIdentificator", instalment);
        transaction.put("chargeNumber", 3);
        transaction.put("billId", billId);
        transaction.put("transactionName", "Notebook");
        transaction.put("transactionId", "tx-" + instalment);
        return transaction;
    }

    @Test
    void theConsentReadFromTheOfficialSchemaSaysWhetherTheBankStillAuthorisesIt() throws IOException {
        ObjectNode consent = (ObjectNode) data("200ConsentsConsentIdRead");

        // The official example is AWAITING_AUTHORISATION: not usable, but not ended either.
        assertThrows(com.github.yildizmy.exception.OpenFinanceUnavailableException.class,
                () -> OfbResponses.requireAuthorised(consent));

        consent.put("status", "REJECTED");
        var ended = assertThrows(com.github.yildizmy.exception.OpenFinanceConsentEndedException.class,
                () -> OfbResponses.requireAuthorised(consent));
        assertEquals("USER", ended.getRejectedBy());
        assertEquals("CONSENT_EXPIRED", ended.getReason());

        consent.put("status", "AUTHORISED");
        assertDoesNotThrow(() -> OfbResponses.requireAuthorised(consent));
    }

    private static JsonNode data(String fixture) throws IOException {
        try (InputStream input = OfbResponsesTest.class.getResourceAsStream("/ofb/" + fixture + ".json")) {
            return JSON.readTree(input).path("data");
        }
    }
}
