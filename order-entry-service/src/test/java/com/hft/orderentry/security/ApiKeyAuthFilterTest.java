package com.hft.orderentry.security;

import com.hft.orderentry.entity.TraderAccount;
import com.hft.orderentry.repository.TraderAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock private TraderAccountRepository traderAccountRepository;
    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter(traderAccountRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKey_setsAuthenticationAndContinuesChain() throws Exception {
        TraderAccount account = new TraderAccount();
        account.setAccountId("test-account-001");
        account.setApiKey("test-api-key-001");
        when(traderAccountRepository.findByApiKey("test-api-key-001")).thenReturn(Optional.of(account));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "test-api-key-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test-account-001", SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(chain.getRequest(), "filter chain must be continued");
    }

    @Test
    void missingApiKeyHeader_noAuthSet_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(traderAccountRepository, never()).findByApiKey(any());
        assertNotNull(chain.getRequest());
    }

    @Test
    void invalidApiKey_noAuthSet_chainContinues() throws Exception {
        when(traderAccountRepository.findByApiKey("bad-key")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "bad-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}
