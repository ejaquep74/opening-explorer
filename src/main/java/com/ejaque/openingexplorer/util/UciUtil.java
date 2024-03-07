package com.ejaque.openingexplorer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ejaque.openingexplorer.config.Constants;

public class UciUtil {

    private static Pattern SCORE_PATTERN = Pattern.compile("score cp (-?\\d+)|score mate (-?\\d+)");

    /**
     * Gets the color to play from the FEN string.
     * @param fenCode The FEN string of the position.
     * @return The color to play.
     */
    public static String getColorToPlay(String fenCode) {
        String[] parts = fenCode.split(" ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid FEN string: " + fenCode);
        }
        return parts[1].equals("w") ? Constants.COLOR_WHITE : Constants.COLOR_BLACK;
    }

    /**
     * Gets the evaluation of the position from a UCI info message.
     * @param fenCode The FEN string of the position.
     * @param uciInfoMessage The UCI info message containing the evaluation.
     * @return The evaluation score, positive for White's advantage, negative for Black's advantage.
     * @throws IllegalArgumentException if the UCI info message does not contain a valid score.
     */
    public static double getEval(String fenCode, String uciInfoMessage) throws IllegalArgumentException {
        Matcher matcher = SCORE_PATTERN.matcher(uciInfoMessage);

        if (matcher.find()) {
            int multiplier = getColorToPlay(fenCode).equals(Constants.COLOR_WHITE) ? 1 : -1;

            if (matcher.group(1) != null) {
                // Centipawn score
                int scoreCp = Integer.parseInt(matcher.group(1));
                return multiplier * (scoreCp / 100.0); // Convert to a more readable format
            } else if (matcher.group(2) != null) {
                // Mate score
                int mateIn = Integer.parseInt(matcher.group(2));
                double score = mateIn > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                return multiplier * score;
            }
        }

        throw new IllegalArgumentException("Invalid UCI info message: No valid score found.");
    }

    /**
     * Extracts the best move from a UCI engine message starting with "bestmove".
     *
     * @param bestMoveMessage The UCI engine best move message.
     * @return The best move in UCI format (e.g., "e2e4").
     * @throws IllegalArgumentException If the message does not contain a valid move.
     */
    public static String getBestMove(String bestMoveMessage) throws IllegalArgumentException {
        if (bestMoveMessage.startsWith("bestmove")) {
            String[] parts = bestMoveMessage.split(" ");
            if (parts.length >= 2) {
                return parts[1];
            }
        }

        throw new IllegalArgumentException("Invalid best move message: No valid move found.");
    }
}
