package com.chacha.multitenantsaas.search.spi;

import java.util.List;

public interface GlobalSearchContributor {
    List<GlobalSearchHit> search(GlobalSearchContext context, String normalizedQuery, int limit);
}
