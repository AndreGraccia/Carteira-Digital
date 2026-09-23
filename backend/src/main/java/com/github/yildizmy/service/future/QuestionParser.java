package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a free-text question into one of the questions the engine can answer.
 * <p>
 * Deterministic on purpose: the answer is always built from engine numbers, so the parser only has to
 * pick the right calculation and pull an amount and an instalment count out of the text. A language
 * model may later replace this class to understand more phrasings; it would still only choose among
 * these intents and never produce the numbers (ADR 0002).
 */
public final class QuestionParser {

    // ponytail: keyword lists in PT and EN cover the suggested questions and common variations.
    // Swap for an LLM intent classifier when real usage shows phrasings these miss.
    private static final Pattern INSTALLMENTS = Pattern.compile("(\\d{1,2})\\s*(x|vezes|parcelas?|installments?)\\b");
    private static final Pattern AMOUNT = Pattern.compile(
            "(r\\$\\s*)?(\\d{1,3}(?:[.\\s]\\d{3})+(?:,\\d{1,2})?|\\d+\\.\\d{1,2}(?!\\d)|\\d+(?:,\\d{1,2})?)\\s*(mil|k)?\\b");
    private static final Pattern PURCHASE = Pattern.compile(
            "\\b(comprar|compro|compra|parcelar|pagar|afford|buy|purchase)\\b");
    private static final Pattern SPEND = Pattern.compile("\\b(gastar|gasto|spend)\\b");
    private static final Pattern DAILY = Pattern.compile(
            "por dia|ao dia|quanto (eu )?posso gastar|posso gastar hoje|limite|per day|a day|how much can i spend");
    private static final Pattern WHY = Pattern.compile(
            "por ?que|negativ|vermelho|faltar|falta dinheiro|buraco|why|negative|short|overdraft|cheque especial");
    private static final Pattern COMMITTED = Pattern.compile(
            "comprometid|destino|compromisso|contas do mes|quanto (ja )?devo|committed|bills|owe");
    private static final Pattern INCOME = Pattern.compile(
            "salario|quando (eu )?recebo|proxim[oa] (entrada|pagamento|renda)|payday|salary|next income|get paid");
    private static final Pattern CARD = Pattern.compile("cartao|credito|card|credit");

    private QuestionParser() {
    }

    public static Parsed parse(String question) {
        String text = normalize(question);

        Integer installments = null;
        Matcher inst = INSTALLMENTS.matcher(text);
        if (inst.find()) {
            installments = Integer.parseInt(inst.group(1));
            text = text.substring(0, inst.start()) + " " + text.substring(inst.end());
        }
        BigDecimal amount = amount(text);
        boolean card = CARD.matcher(text).find() || (installments != null && installments > 1);

        Intent intent;
        if (PURCHASE.matcher(text).find() || (amount != null && SPEND.matcher(text).find() && !DAILY.matcher(text).find())) {
            intent = Intent.PURCHASE;
        } else if (DAILY.matcher(text).find() || SPEND.matcher(text).find()) {
            intent = Intent.DAILY_LIMIT;
        } else if (WHY.matcher(text).find()) {
            intent = Intent.WHY_NEGATIVE;
        } else if (COMMITTED.matcher(text).find()) {
            intent = Intent.COMMITTED;
        } else if (INCOME.matcher(text).find()) {
            intent = Intent.NEXT_INCOME;
        } else {
            intent = Intent.UNKNOWN;
        }
        return new Parsed(intent, amount, installments == null ? 1 : installments, card);
    }

    /** Prefers a value marked with R$; otherwise the largest number in the text. */
    static BigDecimal amount(String text) {
        Matcher matcher = AMOUNT.matcher(text);
        BigDecimal marked = null;
        BigDecimal largest = null;
        while (matcher.find()) {
            String digits = matcher.group(2);
            BigDecimal value;
            if (digits.matches("\\d+\\.\\d{1,2}")) {
                value = new BigDecimal(digits);
            } else {
                value = new BigDecimal(digits.replaceAll("[.\\s]", "").replace(',', '.'));
            }
            if (matcher.group(3) != null) value = value.multiply(BigDecimal.valueOf(1000));
            if (value.signum() <= 0) continue;
            if (matcher.group(1) != null && marked == null) marked = value;
            if (largest == null || value.compareTo(largest) > 0) largest = value;
        }
        BigDecimal result = marked != null ? marked : largest;
        return result == null ? null : result.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    static String normalize(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public enum Intent { PURCHASE, DAILY_LIMIT, WHY_NEGATIVE, COMMITTED, NEXT_INCOME, UNKNOWN }

    /**
     * @param amount       value found in the question, or null
     * @param installments 1 when none was mentioned
     * @param card         the question mentions a card, or splits the payment (instalments are a card thing in Brazil)
     */
    public record Parsed(Intent intent, BigDecimal amount, int installments, boolean card) {
    }
}
