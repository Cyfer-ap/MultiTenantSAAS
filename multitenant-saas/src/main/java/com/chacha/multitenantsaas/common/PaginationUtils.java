package com.chacha.multitenantsaas.common;

public final class PaginationUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static int validatePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 0");
        }

        return page;
    }

    public static int validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }

        return size;
    }
}