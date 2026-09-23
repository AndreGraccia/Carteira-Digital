package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.QuestionParser.Intent;
import com.github.yildizmy.service.future.QuestionParser.Parsed;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Posso comprar um notebook de R$ 4.000 em 10x?        | PURCHASE     | 4000.00 | 10 | true",
            "posso comprar um tênis de 399,90?                    | PURCHASE     | 399.90  | 1  | false",
            "Dá pra parcelar 1.500,50 em 3 vezes no cartão?       | PURCHASE     | 1500.50 | 3  | true",
            "E se eu comprar uma TV de 3 mil?                     | PURCHASE     | 3000.00 | 1  | false",
            "posso gastar 200 no fim de semana?                   | PURCHASE     | 200.00  | 1  | false",
            "Can I afford a 39.90 subscription?                   | PURCHASE     | 39.90   | 1  | false",
            "Quanto posso gastar por dia?                         | DAILY_LIMIT  |         | 1  | false",
            "quanto eu posso gastar hoje                          | DAILY_LIMIT  |         | 1  | false",
            "How much can I spend per day?                        | DAILY_LIMIT  |         | 1  | false",
            "Por que meu saldo fica negativo?                     | WHY_NEGATIVE |         | 1  | false",
            "vai faltar dinheiro esse mês?                        | WHY_NEGATIVE |         | 1  | false",
            "Quanto do meu dinheiro já está comprometido?         | COMMITTED    |         | 1  | false",
            "Quando cai meu salário?                              | NEXT_INCOME  |         | 1  | false",
            "Qual a previsão do tempo?                            | UNKNOWN      |         | 1  | false",
    })
    void understandsTheQuestionsTheEngineCanAnswer(String question, Intent intent, BigDecimal amount,
                                                   int installments, boolean card) {
        Parsed parsed = QuestionParser.parse(question);

        assertEquals(intent, parsed.intent());
        if (amount == null) assertNull(parsed.amount());
        else assertEquals(amount, parsed.amount());
        assertEquals(installments, parsed.installments());
        assertEquals(card, parsed.card());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "r$ 1.234,56 e 99            | 1234.56",
            "de 50 a 700                 | 700.00",
            "4k                          | 4000.00",
            "2 mil e 500                 | 2000.00",
    })
    void findsTheAmount(String text, BigDecimal expected) {
        assertEquals(expected, QuestionParser.amount(QuestionParser.normalize(text)));
    }
}
