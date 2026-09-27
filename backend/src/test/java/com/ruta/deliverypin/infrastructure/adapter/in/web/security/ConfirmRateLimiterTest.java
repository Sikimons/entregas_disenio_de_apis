package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.exception.TooManyConfirmAttemptsException;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmRateLimiterTest {

    private ConfirmRateLimiter limiterWithMaxAttempts(int maxAttempts) {
        AppProperties properties = new AppProperties();
        properties.getConfirmRateLimit().setMaxAttempts(maxAttempts);
        properties.getConfirmRateLimit().setWindowSeconds(60);
        return new ConfirmRateLimiter(properties);
    }

    @Test
    void checkAllowed_underLimit_doesNotThrow() {
        ConfirmRateLimiter limiter = limiterWithMaxAttempts(3);

        assertThatCode(() -> {
            limiter.checkAllowed(1L);
            limiter.checkAllowed(1L);
            limiter.checkAllowed(1L);
        }).doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_exceedsLimit_throwsTooManyConfirmAttempts() {
        ConfirmRateLimiter limiter = limiterWithMaxAttempts(3);
        limiter.checkAllowed(1L);
        limiter.checkAllowed(1L);
        limiter.checkAllowed(1L);

        assertThatThrownBy(() -> limiter.checkAllowed(1L))
                .isInstanceOf(TooManyConfirmAttemptsException.class);
    }

    @Test
    void checkAllowed_differentDriver_hasIndependentLimit() {
        ConfirmRateLimiter limiter = limiterWithMaxAttempts(1);
        limiter.checkAllowed(1L);

        assertThatCode(() -> limiter.checkAllowed(2L)).doesNotThrowAnyException();
    }
}
