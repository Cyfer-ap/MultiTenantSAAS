package com.chacha.multitenantsaas.search.spi;

import com.chacha.multitenantsaas.search.model.GlobalSearchResult;

public record GlobalSearchHit(GlobalSearchResult result, int score) {}
