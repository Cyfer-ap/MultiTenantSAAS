package com.chacha.multitenantsaas.service;

import java.net.URI;
import java.util.Map;

public interface OidcHttpTransport {

    Map<String, Object> getJson(URI uri);

    default Map<String, Object> postFormJson(
            URI uri, Map<String, String> form, String basicClientId, String basicClientSecret) {
        throw new UnsupportedOperationException("OIDC form POST is not supported by this transport");
    }
}
