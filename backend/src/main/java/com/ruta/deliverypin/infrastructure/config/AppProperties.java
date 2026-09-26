package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Admin admin = new Admin();

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
}
