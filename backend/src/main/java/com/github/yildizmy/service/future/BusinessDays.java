package com.github.yildizmy.service.future;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Brazilian bank calendar: weekends, national holidays and the movable days banks close (Carnival Monday and
 * Tuesday, Good Friday, Corpus Christi). A bill due on a closed day is paid on the next business day; money
 * coming in is usually paid on the previous one. See ADR 0007.
 * <p>
 * ponytail: national calendar only. State and city holidays (e.g. 25/01 in São Paulo) are not modelled;
 * add a per-user region when the projection needs them.
 */
public final class BusinessDays {

    private static final Set<MonthDay> FIXED = Set.of(
            MonthDay.of(1, 1),   // Confraternização Universal
            MonthDay.of(4, 21),  // Tiradentes
            MonthDay.of(5, 1),   // Dia do Trabalho
            MonthDay.of(9, 7),   // Independência
            MonthDay.of(10, 12), // Nossa Senhora Aparecida
            MonthDay.of(11, 2),  // Finados
            MonthDay.of(11, 15), // Proclamação da República
            MonthDay.of(11, 20), // Consciência Negra (Lei 14.759/2023)
            MonthDay.of(12, 25)  // Natal
    );

    private BusinessDays() {
    }

    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        if (FIXED.contains(MonthDay.from(date))) return false;
        LocalDate easter = easter(date.getYear());
        return !date.equals(easter.minusDays(48))  // Carnaval (segunda)
                && !date.equals(easter.minusDays(47))  // Carnaval (terça)
                && !date.equals(easter.minusDays(2))   // Sexta-feira Santa
                && !date.equals(easter.plusDays(60));  // Corpus Christi
    }

    /** The date itself when it is a business day, otherwise the next one. For bills, invoices and instalments. */
    public static LocalDate onOrAfter(LocalDate date) {
        LocalDate result = date;
        while (!isBusinessDay(result)) result = result.plusDays(1);
        return result;
    }

    /** The date itself when it is a business day, otherwise the previous one. For salaries and other income. */
    public static LocalDate onOrBefore(LocalDate date) {
        LocalDate result = date;
        while (!isBusinessDay(result)) result = result.minusDays(1);
        return result;
    }

    /** Easter Sunday, anonymous Gregorian algorithm (Meeus/Jones/Butcher). */
    static LocalDate easter(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
