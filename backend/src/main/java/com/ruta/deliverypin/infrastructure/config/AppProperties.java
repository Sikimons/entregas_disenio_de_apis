package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Admin admin = new Admin();
    private LoginRateLimit loginRateLimit = new LoginRateLimit();
    private Seed seed = new Seed();

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public LoginRateLimit getLoginRateLimit() {
        return loginRateLimit;
    }

    public void setLoginRateLimit(LoginRateLimit loginRateLimit) {
        this.loginRateLimit = loginRateLimit;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public static class Cors {
        private String allowedOrigins;

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Admin {
        private String bootstrapUsername;
        private String bootstrapPassword;

        public String getBootstrapUsername() {
            return bootstrapUsername;
        }

        public void setBootstrapUsername(String bootstrapUsername) {
            this.bootstrapUsername = bootstrapUsername;
        }

        public String getBootstrapPassword() {
            return bootstrapPassword;
        }

        public void setBootstrapPassword(String bootstrapPassword) {
            this.bootstrapPassword = bootstrapPassword;
        }
    }

    public static class LoginRateLimit {
        private int maxAttempts = 5;
        private int windowSeconds = 60;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    /** Carga de datos de muestra en el arranque (ver infrastructure.seed.Initializer). */
    public static class Seed {
        private boolean enabled = false;
        private String file = "file:/app/demo/invoices.json";
        private boolean oneShot = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public boolean isOneShot() {
            return oneShot;
        }

        public void setOneShot(boolean oneShot) {
            this.oneShot = oneShot;
        }
    }
}
