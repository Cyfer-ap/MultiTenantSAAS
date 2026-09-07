package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboundWebhookUrlValidatorTest {

    @Test
    void acceptsHttpsDestinationWhenEveryResolvedAddressIsPublic() throws Exception {
        HostAddressResolver resolver = host -> List.of(InetAddress.getByName("8.8.8.8"));
        OutboundWebhookUrlValidator validator = new OutboundWebhookUrlValidator(resolver);

        assertThat(validator.validateAndNormalize(" https://hooks.example.com/events "))
                .isEqualTo("https://hooks.example.com/events");
    }

    @Test
    void rejectsNonHttpsDestinations() {
        HostAddressResolver resolver = host -> List.of();
        OutboundWebhookUrlValidator validator = new OutboundWebhookUrlValidator(resolver);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validateAndNormalize("http://hooks.example.com/events"))
                .withMessage("Webhook URL must use HTTPS");
    }

    @Test
    void rejectsLoopbackAndPrivateDestinations() throws Exception {
        HostAddressResolver loopback = host -> List.of(InetAddress.getByName("127.0.0.1"));
        HostAddressResolver privateAddress = host -> List.of(InetAddress.getByName("10.10.10.10"));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                new OutboundWebhookUrlValidator(loopback)
                                        .validateAndNormalize("https://localhost/hooks"))
                .withMessageContaining("must not resolve to private");
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                new OutboundWebhookUrlValidator(privateAddress)
                                        .validateAndNormalize("https://internal.example/hooks"))
                .withMessageContaining("must not resolve to private");
    }

    @Test
    void rejectsDestinationWhenAnyDnsAnswerIsNonPublic() throws Exception {
        HostAddressResolver mixed =
                host ->
                        List.of(
                                InetAddress.getByName("8.8.8.8"),
                                InetAddress.getByName("169.254.169.254"));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                new OutboundWebhookUrlValidator(mixed)
                                        .validateAndNormalize("https://mixed.example/hooks"))
                .withMessageContaining("must not resolve to private");
    }

    @Test
    void rejectsCredentialsAndFragments() {
        HostAddressResolver resolver = host -> List.of();
        OutboundWebhookUrlValidator validator = new OutboundWebhookUrlValidator(resolver);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> validator.validateAndNormalize("https://user:pass@example.com/hooks"))
                .withMessage("Webhook URL must not include user credentials");
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> validator.validateAndNormalize("https://example.com/hooks#fragment"))
                .withMessage("Webhook URL must not include a fragment");
    }
}
