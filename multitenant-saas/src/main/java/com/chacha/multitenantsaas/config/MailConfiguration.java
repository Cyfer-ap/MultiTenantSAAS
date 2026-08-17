package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.email.BrevoEmailSender;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.email.LoggingEmailSender;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {

    @Bean
    public EmailSender emailSender(MailProperties properties) {
        String provider = normalizeProvider(properties.getProvider());
        return switch (provider) {
            case "logging" -> new LoggingEmailSender();
            case "brevo" -> new BrevoEmailSender(properties);
            default -> throw new IllegalStateException("Unsupported mail provider: " + provider);
        };
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "logging";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
