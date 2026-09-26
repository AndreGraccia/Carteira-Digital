package com.github.yildizmy.controller;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.config.SecurityConfig;
import com.github.yildizmy.security.AuthEntryPointJwt;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.security.UserDetailsServiceImpl;
import com.github.yildizmy.service.ask.AskUsage;
import com.github.yildizmy.service.ForecastService;
import com.github.yildizmy.service.OpenFinanceDailySync;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who reaches the admin routes, with the real security chain and method security; only the services are fake. */
@WebMvcTest({AdminAuditController.class, AdminMetricsController.class})
@Import({SecurityConfig.class, AuthEntryPointJwt.class,
        com.github.yildizmy.config.ClockConfig.class, com.github.yildizmy.config.AppConfig.class})
@TestPropertySource(properties = "app.security.allowedOrigin=http://localhost:3001")
class AdminControllersWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean ForecastService forecastService;
    @MockitoBean AskUsage askUsage;
    @MockitoBean OpenFinanceDailySync dailySync;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean MessageSourceConfig messageConfig;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbc; // audit query and the admin audit interceptor

    private static UserDetailsImpl person(String role) {
        return new UserDetailsImpl(7L, "user7", "x", "A", "B", List.of(new SimpleGrantedAuthority(role)));
    }

    private static List<MockHttpServletRequestBuilder> routes() {
        return List.of(get("/api/v1/admin/audit"), get("/api/v1/admin/metrics"), post("/api/v1/admin/open-finance/daily-sync"));
    }

    @Test
    void withoutATokenEveryAdminRouteIs401() throws Exception {
        for (MockHttpServletRequestBuilder route : routes()) mvc.perform(route).andExpect(status().isUnauthorized());
    }

    @Test
    void anOrdinaryUserGets403AndNothingRuns() throws Exception {
        for (MockHttpServletRequestBuilder route : routes()) {
            mvc.perform(route.with(user(person("ROLE_USER")))).andExpect(status().isForbidden());
        }
        verify(dailySync, never()).runDaily();
        verify(forecastService, never()).northStar(anyInt());
    }

    @Test
    void anAdminReadsTheMetricsAndRunsTheSync() throws Exception {
        mvc.perform(get("/api/v1/admin/metrics").with(user(person("ROLE_ADMIN")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/audit").with(user(person("ROLE_ADMIN")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/open-finance/daily-sync").with(user(person("ROLE_ADMIN")))).andExpect(status().isOk());
        verify(dailySync).runDaily();
    }

    @Test
    void anAdminStillCannotAskForAnUnboundedPeriod() throws Exception {
        mvc.perform(get("/api/v1/admin/metrics?days=100000").with(user(person("ROLE_ADMIN"))))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/admin/audit?days=0").with(user(person("ROLE_ADMIN"))))
                .andExpect(status().is4xxClientError());
    }
}
