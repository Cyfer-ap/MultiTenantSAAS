package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OidcIdTokenValidator {

    private static final long CLOCK_SKEW_SECONDS = 60L;
    private static final Set<JWSAlgorithm> ALLOWED_ALGORITHMS =
            Set.of(
                    JWSAlgorithm.RS256,
                    JWSAlgorithm.RS384,
                    JWSAlgorithm.RS512,
                    JWSAlgorithm.ES256,
                    JWSAlgorithm.ES384,
                    JWSAlgorithm.ES512);

    public OidcVerifiedIdentity validate(
            String idToken,
            OidcProviderMetadata metadata,
            String clientId,
            String expectedNonceHash) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(idToken);
            JWSAlgorithm algorithm = signedJwt.getHeader().getAlgorithm();
            if (!ALLOWED_ALGORITHMS.contains(algorithm)
                    || !providerAdvertisesAlgorithm(metadata, algorithm)) {
                throw failed();
            }

            JWKSet jwkSet = JWKSet.parse(metadata.jwkSet());
            JWK jwk = selectJwk(jwkSet, signedJwt.getHeader().getKeyID());
            if (jwk.getAlgorithm() != null && !algorithm.equals(jwk.getAlgorithm())) {
                throw failed();
            }

            JWSVerifier verifier = verifier(jwk, algorithm);
            if (!signedJwt.verify(verifier)) {
                throw failed();
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateStandardClaims(claims, metadata.issuer(), clientId);

            String nonce = claims.getStringClaim("nonce");
            if (nonce == null
                    || !OidcSecuritySupport.constantTimeHashMatches(nonce, expectedNonceHash)) {
                throw failed();
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank() || subject.length() > 512) {
                throw failed();
            }

            String email = claims.getStringClaim("email");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            return new OidcVerifiedIdentity(
                    claims.getIssuer(), subject, email, Boolean.TRUE.equals(emailVerified));
        } catch (AuthenticationFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failed();
        }
    }

    private boolean providerAdvertisesAlgorithm(
            OidcProviderMetadata metadata, JWSAlgorithm algorithm) {
        Object raw = metadata.configuration().get("id_token_signing_alg_values_supported");
        if (!(raw instanceof List<?> values)) {
            return false;
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(algorithm.getName()::equals);
    }

    private JWK selectJwk(JWKSet jwkSet, String keyId) {
        if (keyId != null && !keyId.isBlank()) {
            JWK selected = jwkSet.getKeyByKeyId(keyId);
            if (selected != null) {
                return selected;
            }
            throw failed();
        }
        if (jwkSet.getKeys().size() == 1) {
            return jwkSet.getKeys().getFirst();
        }
        throw failed();
    }

    private JWSVerifier verifier(JWK jwk, JWSAlgorithm algorithm) throws Exception {
        if (jwk instanceof RSAKey rsaKey && algorithm.getName().startsWith("RS")) {
            return new RSASSAVerifier(rsaKey);
        }
        if (jwk instanceof ECKey ecKey && algorithm.getName().startsWith("ES")) {
            return new ECDSAVerifier(ecKey);
        }
        throw failed();
    }

    private void validateStandardClaims(JWTClaimsSet claims, String issuer, String clientId)
            throws Exception {
        if (!issuer.equals(claims.getIssuer())) {
            throw failed();
        }

        List<String> audience = claims.getAudience();
        if (audience == null || audience.isEmpty() || !audience.contains(clientId)) {
            throw failed();
        }
        if (audience.size() > 1 && !clientId.equals(claims.getStringClaim("azp"))) {
            throw failed();
        }

        Instant now = Instant.now();
        Date expiration = claims.getExpirationTime();
        Date issuedAt = claims.getIssueTime();
        if (expiration == null
                || expiration.toInstant().isBefore(now.minusSeconds(CLOCK_SKEW_SECONDS))
                || issuedAt == null
                || issuedAt.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw failed();
        }

        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null
                && notBefore.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw failed();
        }
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
