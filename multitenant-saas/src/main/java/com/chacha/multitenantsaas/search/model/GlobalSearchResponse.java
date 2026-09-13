package com.chacha.multitenantsaas.search.model;

import java.util.List;

public record GlobalSearchResponse(String query, List<GlobalSearchResult> results) {}
