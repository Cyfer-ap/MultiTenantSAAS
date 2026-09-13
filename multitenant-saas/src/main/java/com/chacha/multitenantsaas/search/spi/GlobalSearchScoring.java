package com.chacha.multitenantsaas.search.spi;

import java.util.Locale;

public final class GlobalSearchScoring {

    private GlobalSearchScoring() {}

    public static int score(String query, String primary, String... secondaryValues) {
        int score = scoreValue(query, primary, 400, 300, 200);

        for (String secondary : secondaryValues) {
            score = Math.max(score, scoreValue(query, secondary, 180, 140, 100));
        }

        return score;
    }

    private static int scoreValue(
            String query, String value, int exactScore, int prefixScore, int containsScore) {
        if (value == null) {
            return 0;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(query)) {
            return exactScore;
        }
        if (normalized.startsWith(query)) {
            return prefixScore;
        }
        if (normalized.contains(query)) {
            return containsScore;
        }
        return 0;
    }
}
