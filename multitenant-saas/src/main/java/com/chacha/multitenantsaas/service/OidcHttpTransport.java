package com.chacha.multitenantsaas.service;

import java.net.URI;
import java.util.Map;

public interface OidcHttpTransport {

    Map<String, Object> getJson(URI uri);
}
