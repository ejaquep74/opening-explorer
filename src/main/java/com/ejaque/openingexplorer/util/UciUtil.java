package com.ejaque.openingexplorer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ejaque.openingexplorer.config.Constants;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;

public class UciUtil {

    private static final Pattern SCORE_PATTERN = Pattern.compile("score cp (-?\\d+)|score mate (-?\\d+)");

    private static final Pattern MOVES_PATTERN = Pattern.compile("pv\\s+(.+)");

    
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
    
    

    public static List<Move> detectSacrifices(String fen, List<String> moves) {
        Board board = new Board();
        board.loadFromFen(fen);
        MoveList moveList = new MoveList(fen);
        Piece promotionPiece = null;

        List<Move> sacrifices = new ArrayList<>();
        for (String moveStr : moves) {
        	String moveStrUcase = moveStr.toUpperCase();
        	Square square1 = Square.valueOf(moveStrUcase.substring(0, 2));
        	Square square2 = Square.valueOf(moveStrUcase.substring(2, 4));
        	
        	if (moveStr.length() > 4) {
        		promotionPiece = Piece.valueOf(moveStr.substring(4, 5));
        	} else {
        		promotionPiece = Piece.NONE;
        	}
            Move move = new Move(
            		square1, 
            		square2, 
            		promotionPiece);
            moveList.add(move);
            board.doMove(move);

            if (isSacrifice(move, board)) {
                sacrifices.add(move);
            }
        }
        return sacrifices;
    }

    private static boolean isSacrifice(Move move, Board board) {
        board.undoMove();
        int valueBefore = pieceValue(board.getPiece(move.getFrom()));
        int valueAfter = pieceValue(board.getPiece(move.getTo()));
        board.doMove(move);

        // to be a sacrifice, target square must have a piece and origin square must have a greater piece
        return valueAfter > 0 && valueBefore - valueAfter > 1;
    }

    private static int pieceValue(com.github.bhlangonijr.chesslib.Piece piece) {
        switch (piece) {
            case WHITE_PAWN:
            case BLACK_PAWN:
                return 1;
            case WHITE_KNIGHT:
            case BLACK_KNIGHT:
            case WHITE_BISHOP:
            case BLACK_BISHOP:
                return 3;
            case WHITE_ROOK:
            case BLACK_ROOK:
                return 5;
            case WHITE_QUEEN:
            case BLACK_QUEEN:
                return 9;
            default:
                return 0;
        }
    }
    
    
    /**
     * Extracts the list of moves from a UCI "info depth..." message.
     *
     * @param uciInfoMessage The UCI message containing move information.
     * @return A list of moves or an empty list if no moves are found.
     */
    public static List<String> extractMoves(String uciInfoMessage) {
        Matcher matcher = MOVES_PATTERN.matcher(uciInfoMessage);
        if (matcher.find()) {
            String movesStr = matcher.group(1);
            return Arrays.asList(movesStr.split("\\s+"));
        }
        return Arrays.asList();  // return an empty list if no moves are found
    }

}
