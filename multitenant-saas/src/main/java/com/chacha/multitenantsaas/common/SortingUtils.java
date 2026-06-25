package com.chacha.multitenantsaas.common;

import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

public final class SortingUtils {

    private SortingUtils() {
    }

    public static Sort.Direction getDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }

        throw new IllegalArgumentException("sortDir must be either 'asc' or 'desc'");
    }

    public static String validateSortBy(
            String sortBy,
            String defaultSortBy,
            String... allowedFields
    ) {
        String normalizedSortBy = normalizeSortBy(sortBy, defaultSortBy);

        List<String> allowedFieldList = Arrays.asList(allowedFields);

        if (!allowedFieldList.contains(normalizedSortBy)) {
            throw new IllegalArgumentException(
                    "sortBy must be one of: " + String.join(", ", allowedFields)
            );
        }

        return normalizedSortBy;
    }

    private static String normalizeSortBy(String sortBy, String defaultSortBy) {
        if (sortBy == null || sortBy.trim().isBlank()) {
            return defaultSortBy;
        }

        return sortBy.trim();
    }
}
