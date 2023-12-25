package com.ejaque.openingexplorer.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ejaque.openingexplorer.model.GoodMove;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * The class OpeningExplorerService is designed to interact with an online chess
 * move database (like Lichess) and analyze chess openings. The class is
 * equipped to search for the best moves in given board positions and handle
 * various chess-related functionalities. Provides services for interacting with
 * an external chess API and analyzing chess moves and positions. This class
 * serves as a bridge between the application's chess logic and external data
 * sources like Lichess.
 * 
 * <p>
 * API Interaction:
 * </p>
 * <ul>
 * <li>Interacts with the Lichess API to fetch data about chess moves and
 * positions.</li>
 * <li>Constructs an API URL using the current FEN string and sends an HTTP GET
 * request.</li>
 * <li>Parses the API response to extract move data for further analysis.</li>
 * </ul>
 * 
 * <p>
 * Chess Analysis:
 * </p>
 * <ul>
 * <li>Calculates various metrics such as average ratings, game counts, and move
 * popularity.</li>
 * <li>Applies conditions to identify good moves, considering factors like game
 * count thresholds and move popularity percentages.</li>
 * </ul>
 * 
 * <p>
 * Logging and Exception Handling:
 * </p>
 * <ul>
 * <li>Utilizes log statements to record key actions and decisions throughout
 * the move analysis process.</li>
 * <li>Implements exception handling for potential errors during move execution
 * or API communication.</li>
 * </ul>
 * 
 * <p>
 * This class demonstrates the integration of chess logic with external API
 * data, showcasing a combination of chess algorithms, HTTP communication, and
 * application configuration management.
 * </p>
 */
@Service
@Slf4j
public class OpeningExplorerService {
    public static final String COLOR_WHITE = "white"; 
    public static final String COLOR_BLACK = "black";
    
    /** This avg rating is used as a reference for doing weighted sums, just an optimization.*/
    public static final double AVG_RATING = 2500.0;

    
    List<GoodMove> bestMoves = new ArrayList<>();
    
    
    @Value("${lichess.api.username}")
    private String username;

    @Value("${lichess.api.password}")
    private String password;

    @Value("${searchParams.maxDepthHalfMoves}")
	private int maxDepthHalfMoves;
    
    @Value("${searchParams.startPositionFEN}")
	private String startPositionFEN;

    @Value("${searchParams.startPositionColor}")
	private String startPositionColor;

    
    @Value("${searchParams.maxPopularityPctg}")
	private double maxPopularityPctg;
    
    @Value("${searchParams.minRankForRatingAvg}")    
	private Integer minRankForRatingAvg;

    @Value("${searchParams.playerColor}")    
    private String playerColor;

    @Value("${searchParams.minGamesToChooseGoodMove}")    
    private int minGamesToChooseGoodMove;   

    @Value("${searchParams.minGamesToChooseCandidateMove}")    
    private int minGamesToChooseCandidateMove;

    @Value("${searchParams.minPercentileForRatingAvg}")    
    private double minPercentileForRatingAvg;
    
    
	/**
	 * Minimum probability of the move to happen. This probability is calculated
	 * multiplying the pctg played on each opponent move. For example if the first
	 * move we try for opponent is played 50% of the time and the second move we
	 * tried for opponent is played 30%, then the probability of that position
	 * occurring (assuming our starting position occurs always) is 0.5 x 0.3 = 0.15
	 * meaning 15% chance.
	 */
    @Value("${searchParams.minProbabilityOfMove}")    
	private double minProbabilityOfMove;

    @Value("${searchParams.minGamesToExploreOpponentMove}")    
	private double minGamesToExploreOpponentMove;    
    

    /** Starts the search for good moves. */
    public void startSearch() throws Exception {
    	searchBestMove(startPositionFEN, startPositionColor, maxDepthHalfMoves, 1.0);
    }
    
    /**
	 * Recursively searches for the best moves from the given position, depth, and
	 * probability. It communicates with the Lichess API, processes the response,
	 * and applies various criteria to evaluate moves.
	 * 
	 * @param fen               FEN for current position
	 * @param color             Color to play in this move
	 * @param depth             Remaining depth, for example starts with depth 10
	 *                          and ends with 0 when no further searching can be
	 *                          done.
	 * @param parentProbability Probability of the opponent reaching this line
	 * @throws Exception
	 */
    private void searchBestMove(String fen, String color, int depth, double parentProbability) throws Exception {
        
    	log.debug("Remaining DEPTH=" + depth);
    	if (depth == 0) {
        	log.info("hitting MAX DEPTH... returning");
            return; // Stop recursion at depth 0
        }

        String encodedFen = URLEncoder.encode(fen, "UTF-8");
        
        String apiUrl = "https://explorer.lichess.ovh/lichess?speeds=blitz,rapid,classical&ratings=2200,2500&fen=" + encodedFen;

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                org.apache.http.auth.AuthScope.ANY,
                new org.apache.http.auth.UsernamePasswordCredentials(username, password)
        );

        HttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();

        log.info("Call URL: " + apiUrl);
        log.info("FEN: " + fen);
        HttpGet httpGet = new HttpGet(apiUrl);

        HttpResponse response = httpClient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() == 200) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray movesArray = jsonObject.getAsJsonArray("moves");
            int totalWhiteWins = jsonObject.get("white").getAsInt();
            int totalBlackWins = jsonObject.get("black").getAsInt();
            int totalDraws = jsonObject.get("draws").getAsInt();
            int totalGames = totalWhiteWins + totalBlackWins + totalDraws;

            List<Integer> averageRatings = new ArrayList<>();
            List<Integer> averageRatingRanks = new ArrayList<>();
            double averageRatingForAllMoves = 0.0;
            int totalGamesForAllMoves = 0;
            
            for (int i = 0; i < movesArray.size(); i++) {
                JsonObject moveObject = movesArray.get(i).getAsJsonObject();
            	int averageRating = moveObject.get("averageRating").getAsInt();

                int whiteWins = moveObject.get("white").getAsInt();
                int blackWins = moveObject.get("black").getAsInt();
                int draws = moveObject.get("draws").getAsInt();
                
                int totalGamesMove = whiteWins + blackWins + draws;
            	
            	if (totalGamesMove >= minGamesToChooseCandidateMove) {
            		averageRatings.add(averageRating);
            		totalGamesForAllMoves = totalGamesForAllMoves + totalGamesMove;
            		averageRatingForAllMoves = averageRatingForAllMoves + (double) averageRating * totalGamesMove / AVG_RATING;
            	}
            }
            
            // we multiply for AVG_RATING to cancel out the division we did before (this is only for avoiding numeric overflows).
            averageRatingForAllMoves = averageRatingForAllMoves * AVG_RATING / totalGamesForAllMoves;
            
            
            log.info("averageRatingForAllMoves=" + averageRatingForAllMoves);
            
            averageRatingRanks = rankAverageRatings(averageRatings);
            
            for (int i = 0; i < movesArray.size(); i++) {
                JsonObject moveObject = movesArray.get(i).getAsJsonObject();
                String move = moveObject.get("uci").getAsString();
                
                log.debug("checking move: " + move);
                
                int whiteWins = moveObject.get("white").getAsInt();
                int blackWins = moveObject.get("black").getAsInt();
                int draws = moveObject.get("draws").getAsInt();
                
                int totalGamesMove = whiteWins + blackWins + draws;

                // if total games are very few, we stop iterating (moves are ordered descending on total games played)
            	if (totalGamesMove < minGamesToChooseCandidateMove) {
            		break;
            	}
                
                double popularityPctg = (double) totalGamesMove / totalGames;
                
                double accumulatedProbability = 1.0;
                // if we are checking the opponent move, recalculate the Probability of this move
                if (!color.equals(playerColor)) {
                	accumulatedProbability = parentProbability * popularityPctg;
                } else {
                	accumulatedProbability = parentProbability;
                }
                
                // Check if it's played rarely from PLAYER's side
                if (color.equals(playerColor) && popularityPctg <= maxPopularityPctg) {
                	
                	double ratingPercentile = (1 - ((double) (averageRatingRanks.get(i) - 1) / averageRatingRanks.size())) * 100.0;
                	
                    // Check if it's one of the top-3 moves in terms of rating average
                	// and that it has a "minimum of games" played
                    if (ratingPercentile  >= minPercentileForRatingAvg && totalGamesMove >= minGamesToChooseGoodMove) {
                    	log.debug("good move: " + move);
                    	log.debug("popularity pctg: " + popularityPctg);                    	
                    	log.debug("avg rating rank: " + averageRatingRanks.get(i));
                        bestMoves.add(
                        		GoodMove.builder()
                        		.move(move)
                        		.averageRating(averageRatings.get(i))
                        		.averageRatingForAllMoves(averageRatingForAllMoves)
                        		.ratingRank(averageRatingRanks.get(i))
                        		.ratingPercentile(ratingPercentile)
                        		.fen(fen)
                        		.probabilityOcurring(parentProbability)
                        		.build()
                        		);
                    }

                }

                // if Probability of move is enough and we have "enough games", continue searching recursively
                if (accumulatedProbability >= minProbabilityOfMove && totalGamesMove >= minGamesToExploreOpponentMove) {
                    String newFen = generateNewFen(fen, move);
                    String opponentColor = (color.equals(COLOR_WHITE)) ? COLOR_BLACK : COLOR_WHITE;
                    log.debug("try move: " + move);
                    log.debug("accumulatedProbability: " + accumulatedProbability);
                    searchBestMove(newFen, opponentColor, depth - 1, accumulatedProbability);
                    log.debug("back to FEN: "+ fen);
                }

            
            } // end FOR candidate moves
        }
    }
    
    

	private List<Integer> rankAverageRatings(List<Integer> averageRatings) {
        List<Integer> sortedRatings = new ArrayList<>(averageRatings);
        Collections.sort(sortedRatings, Collections.reverseOrder());

        Map<Integer, Integer> ratingToRank = new HashMap<>();
        int rank = 1;

        for (int i = 0; i < sortedRatings.size(); i++) {
            int rating = sortedRatings.get(i);
            if (!ratingToRank.containsKey(rating)) {
                ratingToRank.put(rating, rank);
            }
            rank++;
        }

        List<Integer> averageRatingRanks = new ArrayList<>();
        for (int rating : averageRatings) {
            int rankForRating = ratingToRank.get(rating);
            averageRatingRanks.add(rankForRating);
        }

        return averageRatingRanks;
    }    

    public void printBestMoves() {
        System.out.println("Best Moves:");
        
        for (GoodMove goodMove : bestMoves) {
        	System.out.println("MOVE: " + goodMove);
		}
    }
    
    /**
	 * Generates a new FEN (Forsyth-Edwards Notation) string after a move. It now
	 * includes a check for castling moves and converts them if necessary.
	 * 
	 * @param fen
	 * @param moveUci
	 * @return
	 */
    private String generateNewFen(String fen, String moveUci) {
        Board board = new Board();
        board.loadFromFen(fen);

        Square from = Square.valueOf(moveUci.substring(0, 2).toUpperCase());
        Square to = Square.valueOf(moveUci.substring(2, 4).toUpperCase());

        Move move;
        if (moveUci.length() > 4) {
            char promotionChar = moveUci.charAt(4);
            boolean isBlack = Character.isLowerCase(promotionChar); // Determine the color based on the case of the promotion character
            Piece promotionPiece = convertPromotionCharToPiece(Character.toLowerCase(promotionChar), isBlack);
            move = new Move(from, to, promotionPiece);
        } else {
        	move = new Move(from, to);
            move = convertToCastlingMoveIfNeeded(move, board, from, to);
        }

        try {
            board.doMove(move);
        } catch (MoveException e) {
            e.printStackTrace();
            // Handle the exception or rethrow as appropriate
        }

        return board.getFen();
    }
    
    
	/**
	 * Receives a castling move like e1a1, e1h1, e8a8 or e8h8 and coverts it to a
	 * "normal move" if the king is moving, for example for "e1a1", converts to
	 * "e1c1" (long castling).
	 * 
	 * @param board
	 * @param from
	 * @param to
	 * @return
	 */
	private Move convertToCastlingMoveIfNeeded(Move move, Board board, Square from, Square to) {
	    
		// quick check to improve performance, if the "from" square is not the king square, we know it is not castling
		if (!move.getFrom().equals(Square.E1) && !move.getFrom().equals(Square.E8)) {
			return move; // just return original move as it does not need "castling transformation"
		}
		
		String fen = board.getFen();
	    // Check if the king is in the correct position for castling
	    boolean isWhiteKingInPosition = board.getPiece(Square.E1) == Piece.WHITE_KING;
	    boolean isBlackKingInPosition = board.getPiece(Square.E8) == Piece.BLACK_KING;

	    // Check if the move is a castling move and if castling is available
	    if (from == Square.E1 && to == Square.H1 && fen.contains("K") && isWhiteKingInPosition) {
	        return new Move("e1g1", Side.WHITE); // White short castle
	    } else if (from == Square.E1 && to == Square.A1 && fen.contains("Q") && isWhiteKingInPosition) {
	        return new Move("e1c1", Side.WHITE); // White long castle
	    } else if (from == Square.E8 && to == Square.H8 && fen.contains("k") && isBlackKingInPosition) {
	        return new Move("e8g8", Side.BLACK); // Black short castle
	    } else if (from == Square.E8 && to == Square.A8 && fen.contains("q") && isBlackKingInPosition) {
	        return new Move("e8c8", Side.BLACK); // Black long castle
	    } else {
	    	return move;
	    }
	}
    
    private static Piece convertPromotionCharToPiece(char promotionChar, boolean isBlack) {
        if (isBlack) {
            switch (promotionChar) {
                case 'q': return Piece.BLACK_QUEEN;
                case 'r': return Piece.BLACK_ROOK;
                case 'b': return Piece.BLACK_BISHOP;
                case 'n': return Piece.BLACK_KNIGHT;
                default: throw new IllegalArgumentException("Invalid promotion piece: " + promotionChar);
            }
        } else {
            switch (promotionChar) {
                case 'q': return Piece.WHITE_QUEEN;
                case 'r': return Piece.WHITE_ROOK;
                case 'b': return Piece.WHITE_BISHOP;
                case 'n': return Piece.WHITE_KNIGHT;
                default: throw new IllegalArgumentException("Invalid promotion piece: " + promotionChar);
            }
        }
    }
    
}
