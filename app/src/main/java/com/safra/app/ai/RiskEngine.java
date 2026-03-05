package com.safra.app.ai;

public class RiskEngine {

    public static int calculateRisk(
            int hour,
            double distanceToNearestPolice, // meters
            int nearbyPoliceCount,          // number of police stations
            double routeDistanceKm,
            boolean isHighCrimeZone
    ) {

        double timeRisk = calculateTimeRisk(hour);
        double policeRisk = calculatePoliceRisk(distanceToNearestPolice, nearbyPoliceCount);
        double routeRisk = calculateRouteRisk(routeDistanceKm);
        double crimeRisk = isHighCrimeZone ? 100 : 0;

        double finalScore =
                (timeRisk * 0.25) +
                        (policeRisk * 0.30) +
                        (routeRisk * 0.10) +
                        (crimeRisk * 0.15) +
                        ((nearbyPoliceCount == 0 ? 100 : 0) * 0.20);

        return (int) Math.min(finalScore, 100);
    }

    private static double calculateTimeRisk(int hour) {
        if (hour >= 22 || hour <= 4) return 90;
        if (hour >= 19) return 60;
        if (hour >= 6 && hour <= 18) return 20;
        return 40;
    }

    private static double calculatePoliceRisk(double distance, int count) {
        double distanceRisk;

        if (distance > 5000) distanceRisk = 90;
        else if (distance > 2000) distanceRisk = 70;
        else if (distance > 1000) distanceRisk = 40;
        else distanceRisk = 20;

        double densityModifier = (count >= 3) ? -20 : 0;

        return Math.max(distanceRisk + densityModifier, 0);
    }

    private static double calculateRouteRisk(double km) {
        if (km > 15) return 80;
        if (km > 7) return 50;
        return 20;
    }
}