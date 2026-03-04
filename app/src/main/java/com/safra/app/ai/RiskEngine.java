package com.safra.app.ai;

public class RiskEngine {

    public static int calculateRisk(
            int hour,
            double distanceToPolice,
            boolean isIsolatedArea,
            boolean isHighCrimeZone
    ) {

        int score = 0;

        // Time factor
        if (hour >= 22 || hour <= 5) {
            score += 30;
        } else if (hour >= 19) {
            score += 15;
        }

        // Distance factor
        if (distanceToPolice > 2000) {  // meters
            score += 25;
        } else if (distanceToPolice > 1000) {
            score += 15;
        }

        // Isolation factor
        if (isIsolatedArea) {
            score += 25;
        }

        // Crime zone factor
        if (isHighCrimeZone) {
            score += 30;
        }

        return Math.min(score, 100);
    }
}