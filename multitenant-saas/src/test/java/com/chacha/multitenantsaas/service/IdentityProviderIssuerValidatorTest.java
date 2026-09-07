package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityProviderIssuerValidatorTest {

    @Test
    void acceptsHttpsIssuerWhenEveryResolvedAddressIsPublic() throws Exception {
        HostAddressResolver resolver = host -> List.of(InetAddress.getByName("8.8.8.8"));
        IdentityProviderIssuerValidator validator = new IdentityProviderIssuerValidator(resolver);

        assertThat(validator.validateAndNormalize(" https://login.example.com/tenant/v2.0 "))
                .isEqualTo("https://login.example.com/tenant/v2.0");
    }

    @Test
    void rejectsNonHttpsIssuer() {
        HostAddressResolver resolver = host -> List.of();
        IdentityProviderIssuerValidator validator = new IdentityProviderIssuerValidator(resolver);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validateAndNormalize("http://login.example.com/tenant"))
                .withMessage("Identity-provider issuer URI must use HTTPS");
    }

    @Test
    void rejectsPrivateLoopbackAndMetadataDestinations() throws Exception {
        for (String address : List.of("127.0.0.1", "10.0.0.5", "169.254.169.254")) {
            HostAddressResolver resolver = host -> List.of(InetAddress.getByName(address));
            IdentityProviderIssuerValidator validator =
                    new IdentityProviderIssuerValidator(resolver);

            assertThatIllegalArgumentException()
                    .isThrownBy(
                            () ->
                                    validator.validateAndNormalize(
                                            "https://identity.internal.example/oidc"))
                    .withMessageContaining("must not resolve to private");
        }
    }

    @Test
    void rejectsIssuerWhenAnyDnsAnswerIsNonPublic() throws Exception {
        HostAddressResolver resolver =
                host ->
                        List.of(
                                InetAddress.getByName("8.8.8.8"),
                                InetAddress.getByName("192.168.1.10"));
        IdentityProviderIssuerValidator validator = new IdentityProviderIssuerValidator(resolver);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validateAndNormalize("https://mixed.example.com/oidc"))
                .withMessageContaining("must not resolve to private");
    }

    @Test
    void rejectsCredentialsQueryAndFragment() {
        HostAddressResolver resolver = host -> List.of();
        IdentityProviderIssuerValidator validator = new IdentityProviderIssuerValidator(resolver);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                validator.validateAndNormalize(
                                        "https://user:pass@login.example.com/oidc"))
                .withMessageContaining("must not include user credentials");
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                validator.validateAndNormalize(
                                        "https://login.example.com/oidc?tenant=one"))
                .withMessageContaining("must not include a query string");
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                validator.validateAndNormalize(
                                        "https://login.example.com/oidc#fragment"))
                .withMessageContaining("must not include a fragment");
    }
}
