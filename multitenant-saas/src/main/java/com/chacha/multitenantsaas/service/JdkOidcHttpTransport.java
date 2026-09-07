package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.exception.IdentityProviderVerificationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JdkOidcHttpTransport implements OidcHttpTransport {

    static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    @Autowired
    public JdkOidcHttpTransport(JsonMapper jsonMapper) {
        this(
                jsonMapper,
                HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build());
    }

    JdkOidcHttpTransport(JsonMapper jsonMapper, HttpClient httpClient) {
        this.jsonMapper = jsonMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Map<String, Object> getJson(URI uri) {
        HttpRequest request =
                HttpRequest.newBuilder(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        return sendJson(request);
    }

    @Override
    public Map<String, Object> postFormJson(
            URI uri, Map<String, String> form, String basicClientId, String basicClientSecret) {
        String body =
                form.entrySet().stream()
                        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                        .collect(Collectors.joining("&"));
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (basicClientId != null && basicClientSecret != null) {
            String credentials = encode(basicClientId) + ":" + encode(basicClientSecret);
            String encoded =
                    Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }
        return sendJson(builder.build());
    }

    private Map<String, Object> sendJson(HttpRequest request) {
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                closeQuietly(response.body());
                throw new IdentityProviderVerificationException(
                        "OIDC provider endpoint returned HTTP " + response.statusCode());
            }

            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) {
                    throw new IdentityProviderVerificationException(
                            "OIDC provider response exceeded the 1 MiB safety limit");
                }
                if (bytes.length == 0) {
                    throw new IdentityProviderVerificationException(
                            "OIDC provider returned an empty JSON response");
                }
                return jsonMapper.readValue(bytes, MAP_TYPE);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdentityProviderVerificationException(
                    "OIDC provider request was interrupted", exception);
        } catch (IdentityProviderVerificationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new IdentityProviderVerificationException(
                    "OIDC provider request or JSON parsing failed", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void closeQuietly(InputStream body) {
        if (body == null) {
            return;
        }
        try {
            body.close();
        } catch (IOException ignored) {
            // The response is already being rejected; there is no useful recovery action here.
        }
    }
}
