package com.github.yildizmy.service;

import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenFinanceDailySyncTest {

    private final OpenFinanceConnectionRepository connections = mock(OpenFinanceConnectionRepository.class);
    private final OpenFinanceService openFinance = mock(OpenFinanceService.class);
    private final ForecastService forecast = mock(ForecastService.class);
    private final OpenFinanceDailySync job = new OpenFinanceDailySync(connections, openFinance, forecast,
            com.github.yildizmy.service.job.JobLock.inProcess());

    @Test
    void anExpiredConsentIsSkippedAndTheOthersAreSyncedAndRecorded() {
        when(connections.findUserIdsByStatus(OpenFinanceConnectionStatus.AUTHORISED)).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new CodedException("OPEN_FINANCE_CONSENT_REQUIRED", HttpStatus.PRECONDITION_REQUIRED))
                .when(openFinance).sync(2L);
        doThrow(new IllegalStateException("snapshot")).when(forecast).record(3L);

        OpenFinanceDailySync.Result result = job.runDaily();

        assertEquals(new OpenFinanceDailySync.Result(2, 1), result);
        verify(forecast).record(1L);
        verify(forecast, never()).record(2L);
    }
}
