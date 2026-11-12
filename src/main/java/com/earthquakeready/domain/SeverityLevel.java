package com.earthquakeready.domain;

public enum SeverityLevel {
    LOW, MODERATE, HIGH, CRITICAL;

    public static SeverityLevel fromScore(int score) {
        if (score < 25) return LOW;
        if (score < 50) return MODERATE;
        if (score < 75) return HIGH;
        return CRITICAL;
    }
}
