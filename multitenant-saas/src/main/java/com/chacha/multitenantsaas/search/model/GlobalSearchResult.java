package com.chacha.multitenantsaas.search.model;

import java.util.UUID;

public record GlobalSearchResult(
        GlobalSearchResultType type,
        UUID id,
        UUID parentId,
        String title,
        String subtitle) {}
