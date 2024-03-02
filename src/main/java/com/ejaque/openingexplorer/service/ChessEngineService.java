package com.ejaque.openingexplorer.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * Service that evaluates positions, connects internally with Chessify.
 */
@Slf4j
public class ChessEngineService {

    private WebSocket webSocket;

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
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence message, boolean last) {
                log.debug("Message received: " + message);
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

    public void sendCommands() {
    	log.debug("Sending commands...");
        sendCommand("stop");
        sendCommand("setoption name MultiPV value 3");
        sendCommand("position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        sendCommand("go infinite");
    }

    private void sendCommand(String command) {
    	log.debug("sendCommand: {}", command);
        webSocket.sendText(command, true);
    }

    public static void main(String[] args) throws Exception {
    	log.info("START..");
    	ChessEngineService client = new ChessEngineService();
        client.createChessEngineServer();
        client.startWSSConnection();
        client.sendCommands();
    }
}
