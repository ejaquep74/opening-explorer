package com.ejaque.openingexplorer.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Precision;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.ejaque.openingexplorer.config.Constants;
import com.ejaque.openingexplorer.event.EvaluationResultEvent;
import com.ejaque.openingexplorer.model.EvaluationResult;
import com.ejaque.openingexplorer.model.GoodMove;
import com.ejaque.openingexplorer.util.EloUtil;
import com.ejaque.openingexplorer.util.PgnUtil;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
    
	/**
	 * Time in millis to wait between calls to Lichess API. Configure this to avoid
	 * getting Http Error 429 from Lichess API. Have seen issues when calling more
	 * often than one transaction per second.
	 */
    @Value("${throttling.minTimeBetweenCalls}")
	private long minTimeBetweenCalls = 1100;
    
    @Autowired
    private ChessEngineService chessEngineService;

    @Autowired
    private ExcelExportService excelExportService;
    
    
    List<GoodMove> bestMoves = new ArrayList<>();
    
    long lastTimeCalledLichess = System.currentTimeMillis();
    
    @Value("${lichess.api.username}")
    private String username;

    @Value("${lichess.api.password}")
    private String password;

    @Value("${searchParams.maxDepthHalfMoves}")
	private int maxDepthHalfMoves;
    
    @Value("${searchParams.startPositionFEN}")
	private String startPositionFEN;
    
	/**
	 * Engine eval for starting position, it is very important as it is used as
	 * reference for avoiding "bad moves" when searching.
	 */
    private double startPositionEval = 0.0;
 
	private String startPositionColor;
    
    @Value("${searchParams.maxPopularityPctg}")
	private double maxPopularityPctg;
    
    @Value("${searchParams.minRankForRatingAvg}")    
	private Integer minRankForRatingAvg;

    private String playerColor;

    @Value("${searchParams.minRatingRatio}")    
    private double minRatingRatio;   
    
    @Value("${searchParams.minGamesToChooseGoodMove}")    
    private int minGamesToChooseGoodMove;   

    @Value("${searchParams.minGamesToChooseCandidateMove}")    
    private int minGamesToChooseCandidateMove;

    @Value("${searchParams.minPercentileForRatingAvg}")    
    private double minPercentileForRatingAvg;
    
    private Integer totalGamesStartingPosition;
    
	/**
	 * Maximum difference with best move for the player (is a positive value). For
	 * example if best move for our WHITE player is 0.7 and max diff is 0.5, we
	 * tolerate moves with eval >= 0.2. If our player is BLACK, the signs are the
	 * opposite, so best = -0.7 and eval <= -0.2. So in summary (best - eval) <=
	 * {+-}0.5. Where depends on WHITE(+) or BLACK (-).
	 * 
	 * NOTE: this is used for diff with starting position eval and also for diff with the best move's eval in the current position being searched.
	 */
    @Value("${searchParams.maxEvalDiff}")    
    private double maxEvalDiff;  // TODO: review if we should split this in two params (see NOTE above)

    /** Target depth for engine eval. Set to 0 ti DISABLE engine usage.*/
    @Value("${searchParams.evalDepth}")    
	private int evalDepth;    
    
	/**
	 * Minimum probability for the move to happen. This probability is calculated
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
    
    @Value("${searchParams.ratingRange}")    
	private String ratingRange;
	private int totalErrorsExploringMoves;
    
    

    /** Starts the search for good moves. */
    public void startSearch() throws Exception {

    	EvaluationResult evaluationResult = null;
    	String nextBestMove = null;
    	
    	// start the chess engine service for evaluations
    	if (evalDepth > 0) {
	    	chessEngineService.createChessEngineServer();
	    	chessEngineService.requestEvaluation(startPositionFEN, null, evalDepth);
	    	chessEngineService.startEvaluations();

	    	// this call BLOCKS until result is ready:
	        evaluationResult = chessEngineService.getEvaluationResult(startPositionFEN, null);  

	        startPositionEval = evaluationResult.getEvaluation();
	        nextBestMove = evaluationResult.getBestMove();
    	}
    	

        // define player's color
        playerColor = PgnUtil.getColorToPlay(startPositionFEN);
        startPositionColor = playerColor;
        
        log.info("playerColor={}", playerColor);
        log.info("startPositionColor={}", startPositionColor);
        
        // start exploring moves
    	searchBestMove(startPositionFEN, nextBestMove, startPositionColor, maxDepthHalfMoves, 1.0, false);
    }
    
	/**
	 * Recursively searches for the best moves from the given position, depth, and
	 * probability. It communicates with the Lichess API, processes the response,
	 * and applies various criteria to evaluate moves.
	 * 
	 * @param fen               FEN for current position
	 * @param engineBestMove    Best move in this position according to engine  TODO: not used, review
	 * @param color             Color to play in this move
	 * @param remainingDepth    Remaining depth, for example starts with depth 10
	 *                          and ends with 0 when no further searching can be
	 *                          done.
	 * @param parentProbability Probability of the opponent reaching this line
	 * @param isExtraDepthCall  Set to TRUE only when you are already at remaining
	 *                          depth 1 but you want the extra call just to get the
	 *                          avg rating returned by this method.
	 * @return Avg rating for all valid moves in this position
	 * @throws Exception
	 */
	private double searchBestMove(String fen, String engineBestMove, String color, int remainingDepth,
			double parentProbability, boolean isExtraDepthCall) throws Exception {

    	double avgRatingForAllValidMoves = 0.0;  	// all "valid" moves that have a minimum games played
    	double avgRatingForAllMoves = 0.0;			// all "moves", for doing stats		
    	List<GoodMove> goodMovesFound = new ArrayList<>();
    	
    	log.debug("Remaining DEPTH=" + remainingDepth);
    	if (remainingDepth == 0 && !isExtraDepthCall) {
        	log.info("hitting MAX DEPTH... returning");
            return avgRatingForAllValidMoves; // Stop recursion at remaining depth = 0
        }
    	
        avgRatingForAllMoves = callLichessApiPositionStats(fen, engineBestMove, color, remainingDepth, parentProbability,
				isExtraDepthCall, avgRatingForAllValidMoves, avgRatingForAllMoves, goodMovesFound);
        
        return avgRatingForAllMoves;
    }

	
	double getEval(String fenCode, String move) {
		chessEngineService.requestEvaluation(fenCode, move, evalDepth);
		chessEngineService.startEvaluations();
		return chessEngineService.getEvaluationResult(fenCode, move).getEvaluation();
	}
	
    /**
     * 
     * @param fen
     * @param engineBestMove          Best move in this position according to engine   TODO: not used, review
     * @param color
     * @param remainingDepth
     * @param parentProbability
     * @param isExtraDepthCall
     * @param avgRatingForAllValidMoves
     * @param avgRatingForAllMoves
     * @param goodMovesFound EMPTY if we are to find this moves, and NOT EMPTY if they are already found and we just need to add stats to them.
     * @return
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws IOException
     * @throws ClientProtocolException
     * @throws Exception
     */
	private double callLichessApiPositionStats(String fen, String engineBestMove, String color, int remainingDepth, double parentProbability,
			boolean isExtraDepthCall, double avgRatingForAllValidMoves, double avgRatingForAllMoves, List<GoodMove> goodMovesFound)
			throws UnsupportedEncodingException, InterruptedException, IOException, ClientProtocolException, Exception {
		
		String encodedFen = URLEncoder.encode(fen, "UTF-8");
		
		String apiUrl = null;
		
		if (ratingRange.trim().endsWith("masters")) {
			apiUrl = "https://explorer.lichess.ovh/masters?fen=" + encodedFen;
		} else {
			apiUrl = "https://explorer.lichess.ovh/lichess?speeds=blitz,rapid,classical&ratings=" + ratingRange + "&fen=" + encodedFen;
		}

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                org.apache.http.auth.AuthScope.ANY,
                new org.apache.http.auth.UsernamePasswordCredentials(username, password)
        );

        HttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
        
        // HANDLE THROTTLING OF CALLS TO LICHESS  (DONT GET BANNED!!!)
        long elapsedTime = System.currentTimeMillis() - lastTimeCalledLichess;
        if (elapsedTime < minTimeBetweenCalls) {
        	long remainingTime = minTimeBetweenCalls - elapsedTime;
        	Thread.sleep(remainingTime);  						// wait some time to total a full second since last calling URL, to avoid Http Error 429
        }
    	lastTimeCalledLichess = System.currentTimeMillis(); // i am just about to call so I record the time here 
        
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

        	// if not set, calculate the total games for starting position
        	if (totalGamesStartingPosition == null && remainingDepth == maxDepthHalfMoves) {
        		totalGamesStartingPosition = totalGames;
        	}
            
            List<Integer> averageRatings = new ArrayList<>();
            List<Integer> averageRatingRanks = new ArrayList<>();
            int totalGamesForAllValidMoves = 0;	// total considering only moves "searched"
            int totalGamesForAllMoves = 0;
            
            for (int i = 0; i < movesArray.size(); i++) {
                JsonObject moveObject = movesArray.get(i).getAsJsonObject();
            	int averageRating = moveObject.get("averageRating").getAsInt();

                int whiteWins = moveObject.get("white").getAsInt();
                int blackWins = moveObject.get("black").getAsInt();
                int draws = moveObject.get("draws").getAsInt();
                
                int totalGamesMove = whiteWins + blackWins + draws;
            	
                // we calculate the avgRatingForAllValidMoves by doing a weighted avg of the avg rating for all moves
            	if (totalGamesMove >= minGamesToChooseCandidateMove) {
            		averageRatings.add(averageRating);
            		totalGamesForAllValidMoves = totalGamesForAllValidMoves + totalGamesMove;
            		avgRatingForAllValidMoves = avgRatingForAllValidMoves + (double) averageRating * totalGamesMove / AVG_RATING;
            	}

            	totalGamesForAllMoves = totalGamesForAllMoves + totalGamesMove;
            	avgRatingForAllMoves = avgRatingForAllMoves + (double) averageRating * totalGamesMove / AVG_RATING;
            }
            
            // we multiply for AVG_RATING to cancel out the division we did before (this is only for avoiding numeric overflows).
            avgRatingForAllValidMoves = (double) avgRatingForAllValidMoves * AVG_RATING / totalGamesForAllValidMoves;
            avgRatingForAllMoves = (double) avgRatingForAllMoves * AVG_RATING / totalGamesForAllMoves;
            
            log.info("avgRatingForAllValidMoves=" + avgRatingForAllValidMoves);
            log.info("avgRatingForAllMoves=" + avgRatingForAllMoves);
            
            averageRatingRanks = rankAverageRatings(averageRatings);
            
            for (int i = 0; i < movesArray.size(); i++) {
            	
            	boolean isGoodMove = false;
            	double ratingPercentile = 0.0;
            	double averageRatingOpponents = 0.0;
            	
                Double localEval = 0.0;
                boolean skipEvalCheck = true;

            	
                JsonObject moveObject = movesArray.get(i).getAsJsonObject();
                String move = moveObject.get("uci").getAsString();
                
                log.debug("checking move: " + move);
                log.debug("total games (prev move): " + totalGames);
                
                
                int whiteWins = moveObject.get("white").getAsInt();
                int blackWins = moveObject.get("black").getAsInt();
                int draws = moveObject.get("draws").getAsInt();
                
                int totalGamesMove = whiteWins + blackWins + draws;
                
                double whitePointsPctg = (double) (whiteWins * 1 + draws * 0.5) / totalGamesMove; 

                // if total games are very few, we stop iterating (moves are ordered descending on total games played)
            	if (totalGamesMove < minGamesToChooseCandidateMove) {
            		break;
            	}
                
                double popularityPctg = (double) totalGamesMove / totalGames;
                
                // calculate the Probability of reaching this position
                double rawProbability = (double) totalGames / totalGamesStartingPosition;
                
                double accumulatedProbability = 1.0;
                // if we are checking the opponent move, recalculate the Probability of this move
                if (!color.equals(playerColor)) {
                	accumulatedProbability = parentProbability * popularityPctg;
                } else {
                	accumulatedProbability = parentProbability;
                }
                
                // Check if it's played rarely from PLAYER's side
                if (color.equals(playerColor) && popularityPctg <= maxPopularityPctg) {
                	
                	ratingPercentile = (1 - ((double) (averageRatingRanks.get(i) - 1) / averageRatingRanks.size())) * 100.0;
                	
                	double ratingRatio = averageRatings.get(i) / avgRatingForAllMoves;
                	
                    // Check if it's one of the top moves in terms of rating average
                	// and that it has a "minimum of games" played
                    if ( 	// ratingRatio > minRatingRatio (below) 
                    		Precision.compareTo(ratingRatio, minRatingRatio, Constants.EPSILON) > 0
                    		&& totalGamesMove >= minGamesToChooseGoodMove) {
                    	
                    	// TODO: check if could use ratingPercentile  >= minPercentileForRatingAvg  like we did in the past
                    	
                    	isGoodMove = true;
                    	
                    	if (evalDepth > 0) {
                    		localEval = getEval(fen, move);
                    	}
                    	
                    	log.debug("*** GOOD MOVE: move={} eval={}", move, localEval);
                    	log.debug("popularity pctg: " + popularityPctg);                    	
                    	log.debug("avg rating rank: " + averageRatingRanks.get(i));
                    }

                    
                    
                }
                
//                String engineBestMoveFen = PgnUtil.getFinalFen(fen, engineBestMove);
//                
//                // BLOCKING call (in reality should rarely block as this evaluation started before calling this method):
//                EvaluationResult bestMoveEvalResult = chessEngineService.getEvaluationResult(engineBestMoveFen);
//                
//                double bestMoveEval = bestMoveEvalResult.getEvaluation();
                double bestMoveEval = 0.0;  // TODO: not used, check if really need evalDiff bellow...
                
                // BLOCKING call (should block more often if engine is slower than opening exploring):
                //EvaluationResult evaluationResult = chessEngineService.getEvaluationResult(fen, move);  // TODO: check if we need the full object or just the eval as now (best move not used)

                //double currentMoveEval = evaluationResult.getEvaluation();
                //String nextBestMove = evaluationResult.getBestMove();
                
                //double evalDiff = bestMoveEval - currentMoveEval;    // TODO: not used, seems we only need globalEvalDiff            
                
                // for BLACK this diff must be used with negative value (as lesser eval is better for black)
                double currentMaxEvalDiff = color.equals(COLOR_WHITE) ? maxEvalDiff : -maxEvalDiff;
                
                // this is the diff with the starting positions' eval
                //double globalEvalDiff = startPositionEval - currentMoveEval;
                
                log.debug("accumulatedProbability: " + accumulatedProbability);
                
				// if Probability of move is enough and we have "enough games" and decent EVAL,
				// continue searching recursively
                if (accumulatedProbability >= minProbabilityOfMove 
                		&& totalGamesMove >= minGamesToExploreOpponentMove 
                		// && Precision.compareTo(evalDiff, currentMaxEvalDiff, Constants.EPSILON) < 0     // TODO: check if we should check for this diff, seems checking with globalEvalDiff is enough
                	) {
                	
                	// if color is for our player, dont skip the eval check (we want to consider eval for searching deeper)
                	if (color.equals(startPositionColor)) {
                		//skipEvalCheck = false;  //FIXME: disabled for now
                	}

                	// if we dont skip evaluation and we havent done it yet, we do it... 
                	if (!skipEvalCheck && localEval == null) {
                		localEval = getEval(fen, move);
                	}
                	
                	if (skipEvalCheck || Precision.compareTo(startPositionEval - localEval, currentMaxEvalDiff, Constants.EPSILON) < 0) {
                		log.debug("EXPLORING MOVE: color={} move={} localEval={} startPositionalEval={}", color, move, localEval, startPositionEval);
                		log.debug("currentMaxEvalDiff={}", currentMaxEvalDiff);
	                	
	                    String newFen = generateNewFen(fen, move);
	                    String opponentColor = (color.equals(COLOR_WHITE)) ? COLOR_BLACK : COLOR_WHITE;
	                    log.debug("try move: " + move);
	                    if (!isExtraDepthCall) {
	                    	// we mark this as an "extra depth call" only if we are in remaining depth=1 
	                    	// and we are doing and we are looking at a "good move"
	                    	averageRatingOpponents = searchBestMove(newFen, null, opponentColor, remainingDepth - 1, accumulatedProbability, remainingDepth == 1 && isGoodMove);
	                    } else {
	                    	log.debug("not doing call to search more moves, we are just getting the avgRatingForAllValidMoves (extra call)");
	                    }
	                    log.debug("back to FEN: "+ fen);
                	
                	} else {
                		log.debug("DISCARDING MOVE: color={} move={} localEval={} startPositionalEval={}", color, move, localEval, startPositionEval);
                	}
	                    
                // if we dont comply with criteria to search deeper but we HAVE to do 
                // an "extra depth call" (for Good Move stats)...
                } else if (isGoodMove) {
                    String newFen = generateNewFen(fen, move);
                    String opponentColor = (color.equals(COLOR_WHITE)) ? COLOR_BLACK : COLOR_WHITE;
                    log.debug("try move (to get stats): " + move);
                	averageRatingOpponents = searchBestMove(newFen, null, opponentColor, remainingDepth - 1, accumulatedProbability, remainingDepth == 1 && isGoodMove);
                	log.debug("back to FEN (got stats): "+ fen);
                }
                
                // if we are iterating in a "good move", we save it to memory
                if (isGoodMove) {
                    bestMoves.add(
                    		GoodMove.builder()
                    		.move(move)
                    		.totalGames(totalGames)
                    		.evaluation(localEval)
                    		.whitePointsPctg(whitePointsPctg)
                    		.averageRating(averageRatings.get(i))
                    		.averageRatingForAllMoves(avgRatingForAllMoves)
                    		.averageRatingOpponents(averageRatingOpponents)
                    		.ratingRank(averageRatingRanks.get(i))
                    		.ratingPercentile(ratingPercentile)
                    		.popularity(popularityPctg)
                    		.totalGamesMove(totalGamesMove)
                    		.fen(fen)
                    		.probabilityOcurring(parentProbability)
                    		.rawProbability(rawProbability)
                    		.whitePointsPctg(whitePointsPctg)
                    		.performance(EloUtil.getPerformance(avgRatingForAllMoves, whitePointsPctg))
                    		.build()
                    		);
                }

            
            } // end FOR candidate moves
        
        } else {
        	totalErrorsExploringMoves++;
        	log.error("ERROR IN RESPONSE...");
        	log.error("response: " + response);
        }
		return avgRatingForAllMoves;
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

	/**
	 * Exports the good moves found to an excel file.
	 * 
	 * @throws IOException If there's some problem generating excel file.
	 */
	public void exportGoodMoves() throws IOException {
        System.out.println("Best Moves:");
        
        for (GoodMove goodMove : bestMoves) {
        	System.out.println("MOVE: " + goodMove);
		}

        log.info("EXPORTING all good moves to EXCEL file.");
        excelExportService.generateExcel(bestMoves);
        
        if (totalErrorsExploringMoves == 0) {
        	System.out.println("FINISHED OK (no errors)");
        } else {
        	System.out.println("FINISHED WITH ERRORS!!! (total=" + totalErrorsExploringMoves + ")");
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
 
    
    private void requestEvaluationList(String fenCode, JsonArray jsonMoves) {
        // Convert JsonArray to List<String>
        List<String> moves = new ArrayList<>();
        for (JsonElement moveElement : jsonMoves) {
            moves.add(moveElement.getAsString());
        }

        chessEngineService.requestEvaluationList(fenCode, moves, evalDepth);
    }
    
    @EventListener
    public void handleEvaluationResult(EvaluationResultEvent event) {
    	// TODO: not used now, using CompletableFuture for integrating with ChessEngineService
        log.info("Received evaluation result: " + event.getEvaluation());
        // Process the result
    }
    
}
