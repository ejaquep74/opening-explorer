package com.ejaque.openingexplorer.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnnotatedPgnMarkerService {

    /**
     * Saves the annotated PGN to a file.
     *
     * @param inputPgnFilePath  The path to the input PGN file.
     * @param outputPgnFilePath The path to the output PGN file where annotated PGN will be saved.
     * @throws Exception If an error occurs during processing or writing to the file.
     */
    public void saveAnnotatedPgnToFile(String inputPgnFilePath, String outputPgnFilePath) throws Exception {
        String annotatedPgn = markImportantMovesInPgn(inputPgnFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPgnFilePath))) {
            writer.write(annotatedPgn);
        } catch (IOException e) {
            log.error("Error writing the annotated PGN to file: " + outputPgnFilePath, e);
            throw e;
        }
    }
    
    
    public String markImportantMovesInPgn(String pgnFilePath) throws Exception {
        PgnHolder pgn = new PgnHolder(pgnFilePath);
        pgn.loadPgn();

        StringBuilder annotatedPgn = new StringBuilder();
        for (Game game : pgn.getGames()) {

        	// Append game headers only if they and their sub-properties are not null
        	if (game.getRound() != null && game.getRound().getEvent() != null) {
        	    if (game.getRound().getEvent().getName() != null) {
        	        annotatedPgn.append("[Event \"").append(game.getRound().getEvent().getName()).append("\"]\n");
        	    }
        	    if (game.getRound().getEvent().getStartDate() != null) {
        	        annotatedPgn.append("[EventDate \"").append(game.getRound().getEvent().getStartDate()).append("\"]\n");
        	    }
        	    if (game.getRound().getEvent().getEventType() != null) {
        	        annotatedPgn.append("[EventType \"").append(game.getRound().getEvent().getEventType()).append("\"]\n");
        	    }
        	    if (game.getRound().getEvent().getSite() != null) {
        	        annotatedPgn.append("[Site \"").append(game.getRound().getEvent().getSite()).append("\"]\n");
        	    }
        	}

        	if (game.getDate() != null) {
        	    annotatedPgn.append("[Date \"").append(game.getDate()).append("\"]\n");
        	}

        	if (game.getRound() != null && game.getRound().getNumber() > 0) {
        	    annotatedPgn.append("[Round \"").append(game.getRound().getNumber()).append("\"]\n");
        	}

        	if (game.getWhitePlayer() != null) {
        	    annotatedPgn.append("[White \"").append(game.getWhitePlayer()).append("\"]\n");
        	    if (game.getWhitePlayer().getElo() > 0) {
        	        annotatedPgn.append("[WhiteElo \"").append(game.getWhitePlayer().getElo()).append("\"]\n");
        	    }
        	}

        	if (game.getBlackPlayer() != null) {
        	    annotatedPgn.append("[Black \"").append(game.getBlackPlayer()).append("\"]\n");
        	    if (game.getBlackPlayer().getElo() > 0) {
        	        annotatedPgn.append("[BlackElo \"").append(game.getBlackPlayer().getElo()).append("\"]\n");
        	    }
        	}

        	if (game.getResult() != null && game.getResult().getDescription() != null) {
        	    annotatedPgn.append("[Result \"").append(game.getResult().getDescription()).append("\"]\n");
        	}

        	if (game.getAnnotator() != null) {
        	    annotatedPgn.append("[Annotator \"").append(game.getAnnotator()).append("\"]\n");
        	}

        	if (game.getPlyCount() != null) {
        	    annotatedPgn.append("[PlyCount \"").append(game.getPlyCount()).append("\"]\n");
        	}

        	if (game.getEco() != null) {
        	    annotatedPgn.append("[ECO \"").append(game.getEco()).append("\"]\n");
        	}

        	if (game.getOpening() != null) {
        	    annotatedPgn.append("[Opening \"").append(game.getOpening()).append("\"]\n");
        	}

        	annotatedPgn.append("\n");


            Board board = new Board();
            MoveList moveList = game.getHalfMoves();
            String[] moves = moveList.toString().split("\\s+");
            Map<Integer, Map<Integer, MoveList>> gameToVariationsMap = getGameToVariationsMap(game);
//            boolean nextMoveIsImportant = false;

        	int moveCounter = 0;
        	int moveIndex = 0;
            for (String moveStr : moves) {

            	moveCounter++;
            	
            	// the last move is not relevant, as we will never mark this
            	if (moveCounter == moveList.size()) continue;
            	
            	// add the move number
            	if ((moveCounter - 1) % 2 == 0) {
            		annotatedPgn.append((moveCounter - 1) / 2 + 1 + ". ");
            	}
            	
            	int totalVariationsNextMove = Optional.ofNullable(gameToVariationsMap.get(moveCounter + 1))
                        .map(variations -> variations.size())
                        .orElse(0);
            	
            	String moveSan = "";
            	String nextMoveSan = "";
            	
            	if (moveCounter > 1) {
            		nextMoveSan = moveList.get(moveCounter).getSan();
            	}
            	
            	if (moveCounter < moveList.size()) {
            		moveSan = moveList.get(moveCounter - 1).getSan();
            	}
            	
            	log.debug("TOTAL: moveCounter={} moveSan={} totalVariations={}", moveCounter, moveSan, totalVariationsNextMove);
            	
            	if (totalVariationsNextMove > 0 || isMoveWithSymbol(nextMoveSan)) {
                    Move move = new Move(moveStr, board.getSideToMove());
                    board.doMove(move);
                    annotatedPgn.append(moveSan)
                                 .append(" { [%csl R")
                                 .append(getDestinationSquare(move))
                                 .append("]} ");
                } else {
                    board.doMove(new Move(moveStr, board.getSideToMove()));
                    annotatedPgn.append(moveSan).append(" ");
                }
            }
            annotatedPgn.append("\n\n");
        }

        return annotatedPgn.toString();
    }

    
    private Map<Integer, Map<Integer, MoveList>> getGameToVariationsMap(Game game) {

        int index = 0;
        int moveCounter = 0;
        int variantIndex = 0;
        
        Map<Integer, Map<Integer, MoveList>> gameToVariationsMap = new HashMap<Integer, Map<Integer, MoveList>>();
        
        final String[] sanArray = game.getHalfMoves().toSanArray();
        for (int i = 0; i < sanArray.length; i++) {
            String san = sanArray[i];
            index++;
            variantIndex++;

            if (game.getVariations() != null) {
                MoveList var = game.getVariations().get(variantIndex);
                if (var != null) {
                	Map<Integer, MoveList> variationsMap = new HashMap<>();
                	
                	MoveList child = game.getVariations().get(variantIndex);
                	
                	// add current variation
                	variationsMap.put(variantIndex, child);
                	
                	// add nested variations
                    variantIndex = translateVariation(variationsMap, game, var, -1,
                            variantIndex, index, moveCounter);
                    
                    log.debug("ADDING VARIATIONS to move. moveIndex={} variationsMap={}", index, variationsMap);
                    gameToVariationsMap.put(index, variationsMap);
                }
            }
            if (i < sanArray.length - 1 &&
                    index % 2 == 0 && index >= 2) {
                moveCounter++;
            }
        }
        
        return gameToVariationsMap;
    }    
    
	private int translateVariation(Map<Integer, MoveList> returnedVariationMap, Game game, MoveList variation, int parent, int variantIndex, int index,
			int moveCounter) throws MoveConversionException {
		
		log.debug("MoveIndex={} Variation={}", index, variation);
		StringBuilder sb = new StringBuilder();
		final int variantIndexOld = variantIndex;
		if (variation != null) {
			boolean terminated = false;
			sb.append("(");
			int i = 0;
			int mc = moveCounter;
			int idx = index;
			String[] sanArray = variation.toSanArray();
			for (i = 0; i < sanArray.length; i++) {
				String sanMove = sanArray[i];
				if (i == 0) {
					sb.append(mc);
					if (idx % 2 == 0) {
						sb.append("... ");
					} else {
						sb.append(". ");
					}
				}

				variantIndex++;

				sb.append(sanMove);
				sb.append(' ');
				final MoveList child = game.getVariations().get(variantIndex);
				if (child != null) {
					if (i == sanArray.length - 1 && variantIndexOld != child.getParent()) {
						terminated = true;
						sb.append(") ");
					}
					log.debug("ADDING VARIATION.  index={} variation={}", variantIndex, child);
					returnedVariationMap.put(variantIndex, child);
					variantIndex = translateVariation(returnedVariationMap, game, child, variantIndexOld, variantIndex, idx, mc);
				}
				if (idx % 2 == 0 && idx >= 2 && i < sanArray.length - 1) {
					mc++;

					sb.append(mc);
					sb.append(". ");
				}
				idx++;

			}
			if (!terminated) {
				sb.append(") ");
			}

		}
		return variantIndex;
	}
    
    
    private boolean isMoveWithSymbol(String moveStr) {
        return moveStr.contains("!") || moveStr.contains("?");
    }

    private String getDestinationSquare(Move move) {
        return move.getTo().toString().toLowerCase();
    }

    public static void main(String[] args) throws Exception {
        AnnotatedPgnMarkerService service = new AnnotatedPgnMarkerService();
        try {
            String annotatedPgn = service.markImportantMovesInPgn("C:/Users/eajaquep/Documents/tal-korchnoi.pgn");
            System.out.println(annotatedPgn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
