package com.chacha.multitenantsaas.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailMessageTest {

    @Test
    void trimsAddressAndSubject() {
        EmailMessage message =
                new EmailMessage("  user@example.com  ", "  Security code  ", "<p>123456</p>");

        assertThat(message.to()).isEqualTo("user@example.com");
        assertThat(message.subject()).isEqualTo("Security code");
    }

    @Test
    void rejectsBlankContent() {
        assertThatThrownBy(() -> new EmailMessage("user@example.com", "Subject", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("htmlContent");
    }
}
