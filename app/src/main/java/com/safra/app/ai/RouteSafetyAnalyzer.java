package com.safra.app.ai;

public class RouteSafetyAnalyzer {

    public static String getRiskLevel(int riskScore) {

        if (riskScore < 30) {
            return "SAFE";
        } else if (riskScore < 60) {
            return "MODERATE RISK";
        } else {
            return "HIGH RISK";
        }
    }

    public static String getRecommendation(int riskScore) {

        if (riskScore > 60) {
            return "SAFRA AI recommends choosing an alternate safer route.";
        } else {
            return "This route appears reasonably safe.";
        }
    }
}