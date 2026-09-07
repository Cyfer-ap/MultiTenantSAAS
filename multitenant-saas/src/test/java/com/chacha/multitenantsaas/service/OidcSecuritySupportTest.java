package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class OidcSecuritySupportTest {

    @Test
    void producesRfc7636S256PkceChallenge() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertThat(OidcSecuritySupport.pkceS256Challenge(verifier))
                .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void constantTimeHashComparisonAcceptsOnlyMatchingRawValue() {
        String hash = OidcSecuritySupport.sha256Hex("state-value");

        assertThat(OidcSecuritySupport.constantTimeHashMatches("state-value", hash)).isTrue();
        assertThat(OidcSecuritySupport.constantTimeHashMatches("other-state", hash)).isFalse();
        assertThat(OidcSecuritySupport.constantTimeHashMatches("state-value", "not-hex")).isFalse();
    }

    @Test
    void generatedAuthorizationSecretsAreUrlSafeAndHighEntropy() {
        String value = OidcSecuritySupport.randomBase64Url(new SecureRandom(), 32);

        assertThat(value).hasSize(43).matches("^[A-Za-z0-9_-]+$");
    }
}
