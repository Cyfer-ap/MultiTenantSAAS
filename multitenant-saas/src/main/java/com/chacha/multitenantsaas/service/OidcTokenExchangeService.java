package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OidcTokenExchangeService {

    private final OidcHttpTransport httpTransport;
    private final IdentityProviderRemoteUriValidator remoteUriValidator;
    private final OidcLoginProperties properties;

    public OidcTokenExchangeService(
            OidcHttpTransport httpTransport,
            HostAddressResolver hostAddressResolver,
            OidcLoginProperties properties) {
        this.httpTransport = httpTransport;
        this.remoteUriValidator = new IdentityProviderRemoteUriValidator(hostAddressResolver);
        this.properties = properties;
    }

    public String exchange(
            OidcProviderMetadata metadata,
            String clientId,
            String clientSecret,
            String authorizationCode,
            String pkceVerifier) {
        if (authorizationCode == null
                || authorizationCode.isBlank()
                || authorizationCode.length() > 8192) {
            throw failed();
        }

        String tokenEndpoint =
                remoteUriValidator.validateAndNormalize(
                        metadata.tokenEndpoint(), "OIDC token endpoint", true);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", authorizationCode);
        form.put("redirect_uri", properties.requireRedirectUri().toString());
        form.put("code_verifier", pkceVerifier);

        String basicClientId = null;
        String basicSecret = null;
        List<String> authMethods = tokenEndpointAuthMethods(metadata.configuration());
        if (authMethods.contains("client_secret_basic")) {
            basicClientId = clientId;
            basicSecret = clientSecret;
        } else if (authMethods.contains("client_secret_post")) {
            form.put("client_id", clientId);
            form.put("client_secret", clientSecret);
        } else {
            throw failed();
        }

        try {
            Map<String, Object> tokenResponse =
                    httpTransport.postFormJson(
                            URI.create(tokenEndpoint), form, basicClientId, basicSecret);
            Object idToken = tokenResponse.get("id_token");
            if (!(idToken instanceof String value) || value.isBlank()) {
                throw failed();
            }
            return value;
        } catch (AuthenticationFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failed();
        }
    }

    private List<String> tokenEndpointAuthMethods(Map<String, Object> configuration) {
        Object raw = configuration.get("token_endpoint_auth_methods_supported");
        if (raw == null) {
            return List.of("client_secret_basic");
        }
        if (!(raw instanceof List<?> values)) {
            throw failed();
        }
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
