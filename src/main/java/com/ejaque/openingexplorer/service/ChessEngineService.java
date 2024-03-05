package com.ejaque.openingexplorer.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.ejaque.openingexplorer.config.Constants;
import com.ejaque.openingexplorer.model.EvaluationResult;
import com.ejaque.openingexplorer.util.PgnUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * Service that evaluates positions, connects internally with Chessify.
 * <br>
 * NOTE: this is SINGLE THREADED as Chessify websocket channel is single threaded. All evaluations requested are put in a "queue" for completion.
 */
@Service
@Slf4j
public class ChessEngineService {

    private WebSocket webSocket;

	/**
	 * Flag used to mark the current eval as completed. You always have to wait for
	 * this to be completed before requesting a new eval to the UCI engine.
	 */
    private CompletableFuture<Void> evalCompletedFlag = new CompletableFuture<Void>();

    
	/**
	 * Pattern to extract relevant information from the UCI "info" message. Example
	 * message: <br>
	 * info depth 22 seldepth 38 multipv 1 score cp 28 nodes 70508105 nps 15948451
	 * hashfull 47 tbhits 0 time 4421 pv d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5
	 * c1g5 c7c6 e2e3 h7h6
	 */
    private static final Pattern INFO_PATTERN = Pattern.compile(   // FIXME: it has hardcoded depth 25
        "info .* depth 25 .* (cp (-?\\d+)|mate (-?\\d+)) .*");

    /** Delay used before rquesting new eval to engine, might be useful to avoid requesting before the engine has completed previous eval. */
	private static final long DELAY_NEW_EVAL = 500;
    
	/**
	 * This is a map from a Short FEN to Evaluation. Short FEN is normal FEN without
	 * las 2 numbers (halfmove clock and move number).
	 */
    private Map<String, CompletableFuture<EvaluationResult>> shortFenToEvaluationMap = new HashMap<>();


    @Autowired
    private ApplicationEventPublisher eventPublisher;  // NOT USED: we use CompletableFuture instead, to wait for results directly

	/**
	 * Color to play in the position to evaluate (is extracted from the FEN). Can be
	 * {@link Constants#COLOR_WHITE} or {@link Constants#COLOR_BLACK}.
	 */
	private String colorToPlay;
	
    
    
    public String createChessEngineServer() throws Exception {
        String queryParameters = "cores=32&engine=stockfish10&options=" 
            + URLEncoder.encode("{\"engine\":{\"type\":\"option\",\"options\":[\"Stockfish 16\",\"CorChess\"],\"description\":\"Select an engine to run.\"},\"syzygy\":{\"type\":\"boolean\",\"options\":[true,true],\"description\":\"Use Syzygy 6 pieces TB.\"}}", StandardCharsets.UTF_8) 
            + "&plugin=0";
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://chessify.me/billing/order_server?" + queryParameters))
                .header("Authority", "chessify.me")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cookie", "_gcl_au=1.1.381066609.1709328585; _fbp=fb.1.1709328585304.867533032; _gid=GA1.2.2107996877.1709328585; _ga=GA1.2.502668357.1709328585; _ga_XCG3GMLS4X=GS1.2.1709328585.1.1.1709328587.58.0.0; session_id=8uq6o4d8r371ut05gdv5y01v12wxsjiu; csrftoken=orQqZz3goljZg2qu4mdQhSzWRcu037CT3p2auuR3NuFTnyajjRLxtrTvWlOQMOHG")
                .header("Referer", "https://chessify.me/analysis")
                .header("sec-ch-ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    
    private String getChessEngineWssUrl() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://chessify.me/user_account/user_servers_info"))
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cookie", "_gcl_au=1.1.381066609.1709328585; _fbp=fb.1.1709328585304.867533032; _gid=GA1.2.2107996877.1709328585; _ga=GA1.2.502668357.1709328585; _ga_XCG3GMLS4X=GS1.2.1709328585.1.1.1709328587.58.0.0; session_id=8uq6o4d8r371ut05gdv5y01v12wxsjiu; csrftoken=orQqZz3goljZg2qu4mdQhSzWRcu037CT3p2auuR3NuFTnyajjRLxtrTvWlOQMOHG")
                .header("Referer", "https://chessify.me/analysis")
                .header("sec-ch-ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        String wssUrl = jsonResponse.getAsJsonObject("servers")
                                     .getAsJsonArray("stockfish10")
                                     .get(1).getAsString();

        return wssUrl;
    }
    
    
    public void startWSSConnection() throws Exception {
    	
    	String uri = getChessEngineWssUrl();
    	
        log.debug("startWSSConnection: {}", uri);
        HttpClient client = HttpClient.newBuilder()
                                      .executor(Executors.newFixedThreadPool(2))
                                      .build();

        WebSocket.Builder builder = client.newWebSocketBuilder();

        builder.header("Pragma", "no-cache")
               .header("Origin", "https://chessify.me")
               .header("Accept-Language", "en-US,en;q=0.9")
               .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
               .header("Cookie", "_gcl_au=1.1.381066609.1709328585; _fbp=fb.1.1709328585304.867533032; _gid=GA1.2.2107996877.1709328585; _ga=GA1.2.502668357.1709328585; _ga_XCG3GMLS4X=GS1.2.1709328585.1.1.1709328587.58.0.0; session_id=8uq6o4d8r371ut05gdv5y01v12wxsjiu; csrftoken=orQqZz3goljZg2qu4mdQhSzWRcu037CT3p2auuR3NuFTnyajjRLxtrTvWlOQMOHG");

        webSocket = builder.buildAsync(URI.create(uri), new WebSocket.Listener() {

			@Override
            public void onOpen(WebSocket webSocket) {
                log.debug("WebSocket opened");
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                log.debug("WebSocket closed: [" + statusCode + "] " + reason);  
                return null;
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence charMessage, boolean last) {
                log.debug("Message received: " + charMessage);
                
                String message = charMessage.toString();
                
                // Check for 'bestmove' command to mark evaluation completion
                if (message.startsWith("info depth")) {
                    evalCompletedFlag.complete(null); // Mark the current evaluation as completed (unblocks waiting threads)
                    evalCompletedFlag = new CompletableFuture<>(); // Reset for the next evaluation
                    //sendCommand("quit");
                    
                    // Close the current WebSocket connection
                    if (webSocket != null) {
                        CompletableFuture<Void> closeFuture = new CompletableFuture<>();
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Resetting connection").thenRun(() -> closeFuture.complete(null));
                        closeFuture.join(); // Wait for the closure to complete
                    }

                    // Reinitialize the WebSocket connection
                    try {
						startWSSConnection();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                    // Re-send any necessary initialization commands
                    sendInitCommands("rnbqkbnr/1ppppppp/p7/8/1P6/P7/2PPPPPP/RNBQKBNR b KQkq - 0 2");
                   
                    
//                    sendCommand("stop");
//                    
//                    try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						log.error("error with thread sleep", e);
//					}
//                    
//                    sendCommand("setoption name MultiPV value 1");
//                    //sendCommand("newucigame");
//                    sendCommand("position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
//                    sendCommand("go depth infinite");

                    return null;
                }
                
                Matcher matcher = INFO_PATTERN.matcher(message);
                if (matcher.find()) {
                    if (matcher.group(1).startsWith("cp")) {  // even if it is lowerbound or upperbound
                        // Convert centipawn score to double
                        double score = Integer.parseInt(matcher.group(2)) / 100.0;
                        log.debug("Converted cp score: " + score);
                    } else if (matcher.group(1).startsWith("mate")) {
                    	
                    	// if negative means mate for black
                    	int mateMoves = Integer.parseInt(matcher.group(3));
                    	
                        // Convert mate score
                        double score = mateMoves > 0.0 ? 100.00 : -100.00;
                        log.debug("Mate detected. Score: " + score);
                    }
                }
                
                
                webSocket.request(1); // Requesting next message
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                log.error("Error on WebSocket: " + error.getMessage());
            }
        }).join();

        if (webSocket == null) {
            throw new RuntimeException("WebSocket connection failed");
        }    
    }
    
    public void sendInitCommands(String fenCode) {
    	log.debug("Sending INIT commands...");
        sendCommand("stop");
        sendCommand("setoption name MultiPV value 1");
        //sendCommand("position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        sendCommand("position fen " + fenCode);        
        sendCommand("go infinite");
        
        // Initialize evalCompletedFlag to a completed state
        // FIXME: evalCompletedFlag = CompletableFuture.completedFuture(null);
    }

    public void sendCommand(String command) {
    	log.debug("sendCommand: {}", command);
        webSocket.sendText(command, true);
    }

    private void saveEvaluation(String fenCode, EvaluationResult evaluationResult) {
    	
    	String shortFenCode = PgnUtil.getShortFenCode(fenCode);
    	
    	shortFenToEvaluationMap.computeIfPresent(shortFenCode, (key, future) -> {
            future.complete(evaluationResult);
            return future;
        });
    }
    
	/**
	 * Request evaluating a list for moves in a position
	 * 
	 * @param fenCode Base position
	 * @param moves   Moves made from the base position (fenCode), in UCI format
	 *                like "e2e4", "g8f3", etc.
	 * @param depth   Max depth to go for the evaluation (in half moves).               
	 */
    public void requestEvaluationList(String fenCode, List<String> moves, int depth) {
    	for (String move : moves) {
			requestEvaluation(fenCode, move, depth);
		}
    }

	/**
	 * Request evaluating a single move in a position.
	 * 
	 * @param fenCode Base position
	 * @param moves   Moves made from the base position (fenCode), in UCI format
	 *                like "e2e4", "g8f3", etc.
	 * @param depth   Max depth to go for the evaluation (in half moves).               
	 */
	private void requestEvaluation(String fenCode, String move, int depth) {
		

    	log.debug("SYNCH before requesting new evaluation...");

        // Queue the evaluation task to be run asynchronously after the current evaluation completes
        evalCompletedFlag = evalCompletedFlag.thenRun(() -> {
        	
        	log.debug("Requesting new evaluation...");
        	
        	try {
				Thread.sleep(DELAY_NEW_EVAL);
			} catch (InterruptedException e) {
				log.error("Error when sleeping before requesting new eval.", e);;
			}
        	
            String finalFen = PgnUtil.getFinalFen(fenCode, move);
            colorToPlay = PgnUtil.getColorToPlay(fenCode);
            
            // Close the current WebSocket connection
            if (webSocket != null) {
                CompletableFuture<Void> closeFuture = new CompletableFuture<>();
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Resetting connection").thenRun(() -> closeFuture.complete(null));
                closeFuture.join(); // Wait for the closure to complete
            }

            // Reinitialize the WebSocket connection
            try {
				startWSSConnection();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            // Re-send any necessary initialization commands
            sendInitCommands("rnbqkb1r/pppppppp/5n2/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2");
        
        });
        
	}

	/**
	 * Gets the evaluation for a position (FEN) in BLOCKING manner, the calling
	 * thread blocks until the evaluation is ready.
	 * 
	 * @param fenCode FEN for the position to evaluate
	 * @return The evaluation
	 */
    public EvaluationResult getEvaluationResult(String fenCode) {
        CompletableFuture<EvaluationResult> evaluationFuture = shortFenToEvaluationMap.computeIfAbsent(fenCode, k -> new CompletableFuture<>());
        return evaluationFuture.join(); // This will block until the future is completed
    }
    
    public static void main(String[] args) throws Exception {
    	log.info("START..");
    	ChessEngineService client = new ChessEngineService();
        client.createChessEngineServer();
        client.startWSSConnection();
        //client.sendInitCommands("rnbqkbnr/ppppppp1/7p/8/6P1/7P/PPPPPP2/RNBQKBNR b KQkq - 0 2");
        
        client.sendInitCommands("r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP1BPPBP/RN1Q1RK1 w - - 8 8");
        
        //client.sendCommand("position fen r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP1BPPBP/RN1Q1RK1 w - - 8 8");
        //client.sendCommand("go depth 25");
        
        List<String> moves = new ArrayList<>();
        moves.add("d1c2");
        //moves.add("d1b3");
        
        //client.requestEvaluationList("r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP1BPPBP/RN1Q1RK1 w - - 8 8", moves, 25);
    }
}
