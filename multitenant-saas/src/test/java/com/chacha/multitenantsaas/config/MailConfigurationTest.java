package com.chacha.multitenantsaas.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.email.LoggingEmailSender;
import org.junit.jupiter.api.Test;

class MailConfigurationTest {

    private final MailConfiguration configuration = new MailConfiguration();

    @Test
    void usesLoggingSenderByDefault() {
        MailProperties properties = new MailProperties();

        assertThat(configuration.emailSender(properties)).isInstanceOf(LoggingEmailSender.class);
    }

    @Test
    void rejectsUnsupportedProvider() {
        MailProperties properties = new MailProperties();
        properties.setProvider("unknown");

        assertThatThrownBy(() -> configuration.emailSender(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported mail provider");
    }

    @Test
    void brevoProviderFailsFastWhenSecretsAreMissing() {
        MailProperties properties = new MailProperties();
        properties.setProvider("brevo");
        properties.setFromEmail("sender@example.com");

        assertThatThrownBy(() -> configuration.emailSender(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }
}
