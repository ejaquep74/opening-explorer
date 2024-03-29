package com.ejaque.openingexplorer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.ejaque.openingexplorer.config.Constants;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.game.Event;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.GameResult;
import com.github.bhlangonijr.chesslib.game.GenericPlayer;
import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.game.Round;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PgnUtil {


//    public static String processLine(String line) {
//        // Regular expression to find 'Z0' or 'z0' inside parentheses
//        String regex = "\\([^()]*[Zz]0[^()]*\\)";
//        
//        // Continuously replace occurrences within parentheses
//        while (line.matches(".*" + regex + ".*")) {
//            line = line.replaceAll(regex, "");
//        }
//
//        // Check if there's a 'Z0' or 'z0' not inside parentheses and remove everything after it
//        int index = line.toLowerCase().indexOf("z0");
//        if (index != -1) {
//            line = line.substring(0, index);
//        }
//
//        return line;
//    }
	
	/**
	 * Gets "Short FEN" which is FEN Code without halfmove clock and move number
	 * (last two digits).
	 * 
	 * @param fenCode
	 * @return
	 */
	public static String getShortFenCode(String fenCode) {
        // Split the FEN string into parts
        String[] parts = fenCode.split(" ");

        // Reconstruct the FEN without the last two parts (halfmove clock and move number)
        // Joining the first 4 parts as they represent
        // 1. Piece placement
        // 2. Active color
        // 3. Castling availability
        // 4. En passant target square
        if (parts.length >= 4) {
            return String.join(" ", parts[0], parts[1], parts[2], parts[3]);
        } else {
        	log.error("Ignoring Error getting the Short FEN for FEN: {}", fenCode);
            // Return the original string if it doesn't have all parts
            return fenCode;        	
        }
	}

	/**
	 * Gets resulting FEN from initial FEN and move done.
	 * 
	 * @param initialFen FEN for initial position
	 * @param moveUci    Move to be made. If NULL, this method just return the
	 *                   initial position
	 * @return
	 * @throws MoveException
	 */
    public static String getFinalFen(String initialFen, String moveUci) throws MoveException {
        
    	// if there's no move, we return FEN for original position
    	if (moveUci == null) {
    		return initialFen;
    	}
    	
    	// Create a new board and load the initial FEN
        Board board = new Board();
        board.loadFromFen(initialFen);

        // Parse the move in UCI format
        Move move = new Move(moveUci, board.getSideToMove());

        // Apply the move to the board
        board.doMove(move);

        // Return the new FEN string
        return board.getFen();
    }
    
    
    public static String getColorToPlay(String fen) {
    	char uciColor = 'x';
        String[] parts = fen.split(" ");
        if (parts.length > 1) {
            uciColor = parts[1].charAt(0);
            
            if (uciColor == 'w') {
            	return Constants.COLOR_WHITE;
            } else if (uciColor == 'b') {
            	return Constants.COLOR_BLACK;
            }
        }
        throw new IllegalArgumentException("Invalid FEN string (trying to get color to play): " + fen);
    }    
    
    
    public static String removeNullMovesFromGame(String pgnLine) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        int z0Depth = Integer.MAX_VALUE;
        int start = -1;
        boolean z0Found = false;
        char currentChar = '\0';
        char prevChar = '\0';
        boolean bracesOpened = false;
        boolean parenthesesOpened = false;
        	
        for (int i = 0; i < pgnLine.length(); i++) {
            currentChar = pgnLine.charAt(i);
            
            // we check if we are opening a bracket (these are pgn comments that can be just added to output without any checks
            if (currentChar == '{') {
            	bracesOpened = true;
            } else if (currentChar == '}') {
            	bracesOpened = false;
            }
            
            // if we keep inside brackets (a pgn comment), keep adding to output without any checks
            // we also check we are in depth above the Z0 depth, because if we are Z0 depth, we do want to process anything)
            if (bracesOpened) {
            	
            	if (depth < z0Depth) {
	            	result.append(currentChar);
	            	continue;
            	
	            // if we are at Z0 depth, we should just ignore and not process this char	
            	} else {
            		continue;
            	}
            }
            

            if (currentChar == '(' && !bracesOpened) {
            	parenthesesOpened = true;
            	if (depth == 0) start = i;
                depth++;
            } else if (prevChar == ')' && !bracesOpened) {
            	parenthesesOpened = false;
            	depth--;
                if (depth == 0) {
                    if (z0Found) {
                        // Reset z0Found and continue without adding to result
                        z0Found = false;
                        z0Depth = Integer.MAX_VALUE;
                        continue;
                    //} else {
                        // If Z0 or z0 was not found, append the whole section
                        //result.append(line.substring(start, i + 1));
                    }
                }
            }

            // Check for "Z0" or "z0"
            if (!z0Found && ((currentChar == 'Z' || currentChar == 'z') && i + 1 < pgnLine.length() && pgnLine.charAt(i + 1) == '0') ) {
                if (depth > 0) {
                    z0Found = true;
                    z0Depth = depth;
                    int lastIndex = result.lastIndexOf("(");
                    if (lastIndex > -1) {
                    	log.info("PGN line has (Z0) section. Line:\n{}", pgnLine);
                    	result.delete(lastIndex, result.length());
                    } else {
                    	log.error("PGN line with no starting parenthesis before Z0\n{}", pgnLine);
                    }
                } else {
                    // Z0 or z0 found outside of any parentheses, return an empty string
                    return "";
                }
            }

            // if we are in the PGN "root" and (we have not found Z0 or above Z0 depth) 
            if (true && (!z0Found || depth < z0Depth)) {
                result.append(currentChar);
                
                // if we detected that we are closing an empty variation like "()"
                // we just delete both parentheses
//                if (prevChar == '(' && currentChar == ')') {
//                	result.delete(result.length() - 2, result.length());
//                }
                
            }
            
            prevChar = currentChar;
        }

        return result.toString();
    }
	
	/**
	 * Process a PGN file to eliminate all variations with Z0 (null moves) or FEN codes (special start positions).
	 * @param inputFilePath
	 * @param outputFilePath
	 */
    public static void processFile(String inputFilePath, String outputFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            
            // to mark that the PGN has some special starting position, we want to IGNORE those
            boolean pgnHasFenPosition = false;
            
            boolean hasEventHeader = false;
            
            while ((line = reader.readLine()) != null) {
                // Skip lines that start with "["
                if (line.startsWith("1")) {  // line with game moves
                	
                	// handle special case when we have no EVENT header
                	if (!hasEventHeader) {
                		writer.write("[Event \"DEFAULT_EVENT_NAME\"]");
                		writer.newLine();
                	}
                	
                	hasEventHeader = false;  // reset this flag as we are ready to process next game
                	
                    if (!pgnHasFenPosition) {
	                	line = removeNullMovesFromGame(line);
	                	writer.newLine();
	                    writer.write(line);
	                    writer.newLine();
	                    writer.newLine();

                    // if we have an special starting position, we IGNORE the game completly
                    } else {
                    	writer.write("");
                    	writer.newLine();
	                    pgnHasFenPosition = false;  // we reset the flag, until next game has FEN header
                    }
                } else if (line.startsWith("[FEN ")) {
                	pgnHasFenPosition = true;
                    writer.write(line);
                    writer.newLine();
                } else if (line.startsWith("[Event ")) {
                	hasEventHeader = true;
                    writer.write(line);
                    writer.newLine();
                } else if (line.startsWith("[")) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Example usage of processFile method
        processFile(
                "C:/Users/eajaquep/Documents/ejp-annotated.pgn", 
        		"C:/Users/eajaquep/Documents/mega-annotated.pgn");        
    }

//    public static String getPgn(String fenCodeCurrEval, List<String> uciMoves) {
//        Board board = new Board();
//        board.loadFromFen(fenCodeCurrEval);
//
//        for (String uciMove : uciMoves) {
//            try {
//                Move move = new Move(Square.valueOf(uciMove.substring(0, 2)), Square.valueOf(uciMove.substring(2, 4)), board.getPiece(Square.valueOf(uciMove.substring(0, 2))));
//                board.doMove(move);
//            } catch (MoveException e) {
//                e.printStackTrace();
//                return "Invalid move sequence";
//            }
//        }
//        
//        return board.getFen() + " " + board.get;
//    }
    
    /**
     * Generates PGN from a given FEN and a list of UCI moves.
     * 
     * @param fenCodeCurrEval The FEN code representing the current evaluation state.
     * @param uciMoves A list of moves in UCI format.
     * @return A string in PGN format representing the game.
     */
    public static String getPgn(String fen, List<String> uciMoves) {

    	Event event = new Event();
        event.setName("Sample Event"); // Set the event name
        event.setSite("Sample Location"); // Set the event location
        event.setStartDate("2024.03.08"); // Set the event start date

        Round round = new Round(event); // Associate the event with the round

        Game game = new Game("gameId", round);
        
        Player whitePlayer = new GenericPlayer("id1", "name1"); // Set the name of the white player
        Player blackPlayer = new GenericPlayer("id2", "name2"); // Set the name of the black player

        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setResult(GameResult.DRAW);

        Board board = new Board();
        board.loadFromFen(fen);

        game.setFen(fen);
        game.setBoard(board);
        
        for (String uciMove : uciMoves) {
        	Piece promotionPiece = null;
        	String moveStrUcase = uciMove.toUpperCase();
        	Square square1 = Square.valueOf(moveStrUcase.substring(0, 2));
        	Square square2 = Square.valueOf(moveStrUcase.substring(2, 4));
        	
        	if (uciMove.length() > 4) {
        		promotionPiece = Piece.valueOf(uciMove.substring(4, 5));
        	} else {
        		promotionPiece = Piece.NONE;
        	}
            Move move = new Move(
            		square1, 
            		square2, 
            		promotionPiece);
            try {
                board.doMove(move);
                //game.setHalfMoves(null);
                //FIXME: game.getHalfMoves().add(move);
            } catch (MoveException e) {
                e.printStackTrace();
            }
        }

        try {
            // Generate PGN from the Game object
            return game.toPgn(true, true); // Parameters can be set for including variations and comments
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
   
}
