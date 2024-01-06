package com.ejaque.openingexplorer.util;

public class EloUtil {

	/**
	 * Calculate the performance
	 * @param elo The players' ELO.
	 * @param pointsPctg The pctg of points won in the tournament.
	 * @return
	 */
    public static double getPerformance(double elo, double pointsPctg) {
        // Check for the edge cases where EA is 0 or 1 to prevent division by zero or logarithm of zero.
        if (pointsPctg == 0) {
            return Double.POSITIVE_INFINITY;
        } else if (pointsPctg == 1) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return elo + 400 * Math.log10(1 / (1 - pointsPctg) - 1);
        }
    }

    public static void main(String[] args) {
        // Example usage
        double EA = 0.6294;
        double performanceRating = getPerformance(2000, EA);
        System.out.println("Performance Rating: " + performanceRating);
    }
}
