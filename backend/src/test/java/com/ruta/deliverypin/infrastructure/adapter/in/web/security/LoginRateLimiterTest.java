package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.exception.TooManyLoginAttemptsException;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private LoginRateLimiter limiterWithMaxAttempts(int maxAttempts) {
        AppProperties properties = new AppProperties();
        properties.getLoginRateLimit().setMaxAttempts(maxAttempts);
        properties.getLoginRateLimit().setWindowSeconds(60);
        return new LoginRateLimiter(properties);
    }

    @Test
    void checkAllowed_underLimit_doesNotThrow() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(3);

        assertThatCode(() -> {
            limiter.checkAllowed("127.0.0.1", "admin");
            limiter.checkAllowed("127.0.0.1", "admin");
            limiter.checkAllowed("127.0.0.1", "admin");
        }).doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_exceedsLimit_throwsTooManyLoginAttempts() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(3);
        limiter.checkAllowed("127.0.0.1", "admin");
        limiter.checkAllowed("127.0.0.1", "admin");
        limiter.checkAllowed("127.0.0.1", "admin");

        assertThatThrownBy(() -> limiter.checkAllowed("127.0.0.1", "admin"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void checkAllowed_differentUsernameSameIp_hasIndependentLimit() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(1);
        limiter.checkAllowed("127.0.0.1", "admin");

        assertThatCode(() -> limiter.checkAllowed("127.0.0.1", "conductor1"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_isCaseInsensitiveOnUsername() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(1);
        limiter.checkAllowed("127.0.0.1", "Admin");

        assertThatThrownBy(() -> limiter.checkAllowed("127.0.0.1", "admin"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void recordSuccess_resetsBucket_soLegitimateUserIsNeverPenalized() {
        // N4 (docs/EVALUACION_TECNICA.md §18): antes, cada login exitoso tambien consumia
        // el cupo, asi que 5 inicios de sesion correctos seguidos bloqueaban al usuario real.
        LoginRateLimiter limiter = limiterWithMaxAttempts(2);
        limiter.checkAllowed("127.0.0.1", "admin");
        limiter.checkAllowed("127.0.0.1", "admin");
        limiter.recordSuccess("127.0.0.1", "admin");

        assertThatCode(() -> {
            limiter.checkAllowed("127.0.0.1", "admin");
            limiter.checkAllowed("127.0.0.1", "admin");
        }).doesNotThrowAnyException();
    }

    @Test
    void recordSuccess_onlyResetsMatchingKey_notOtherUsersOrIps() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(1);
        limiter.checkAllowed("127.0.0.1", "admin");
        limiter.checkAllowed("10.0.0.1", "conductor1");

        limiter.recordSuccess("127.0.0.1", "admin");

        assertThatCode(() -> limiter.checkAllowed("127.0.0.1", "admin")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkAllowed("10.0.0.1", "conductor1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void recordSuccess_isCaseInsensitiveOnUsername() {
        LoginRateLimiter limiter = limiterWithMaxAttempts(1);
        limiter.checkAllowed("127.0.0.1", "Admin");

        limiter.recordSuccess("127.0.0.1", "admin");

        assertThatCode(() -> limiter.checkAllowed("127.0.0.1", "ADMIN")).doesNotThrowAnyException();
    }
}
