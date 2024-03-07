package com.ejaque.openingexplorer.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ejaque.openingexplorer.model.EvaluationResult;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChessEngineServiceTest {

    @Autowired
    private ChessEngineService chessEngineService;
    
    @BeforeEach
    public void init() throws Exception {
        // Initialization logic for ChessEngineService
    	chessEngineService.createChessEngineServer();
//    	chessEngineService.startWSSConnection();
//    	chessEngineService.sendInitCommands("rnbqkbnr/ppppppp1/7p/8/6P1/7P/PPPPPP2/RNBQKBNR b KQkq - 0 2");
    	
//    	// DELETE, checking these commands for now...
//    	chessEngineService.sendCommand("position fen r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP1BPPBP/RN1Q1RK1 w - - 8 8");
//    	chessEngineService.sendCommand("go depth 25");
    }    

    
    @Test
    public void testRequestEvaluationList() throws InterruptedException {
        
    	String fenCode = "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4";
    	EvaluationResult evaluationResult = null;
    	List<String> moves = null;
    	
//    	moves = Arrays.asList("c4c5");
//    	chessEngineService.requestEvaluationList(fenCode, moves, 25);
//        evaluationResult = chessEngineService.getEvaluationResult(fenCode, "c4c5");
//        log.debug("evaluationResult: {} {}", evaluationResult.getEvaluation(), evaluationResult.getBestMove());
    	
        moves = Arrays.asList("c4d5", "g1f3", "c1g5"); 
        CountDownLatch latch = new CountDownLatch(1);
        
        chessEngineService.requestEvaluationList(fenCode, moves, 25);
        
        chessEngineService.startEvaluations();

        evaluationResult = chessEngineService.getEvaluationResult(fenCode, "c4d5");
        log.debug("evaluationResult: {} {}", evaluationResult.getEvaluation(), evaluationResult.getBestMove());
        
        //Thread.sleep(3000);
        evaluationResult = chessEngineService.getEvaluationResult(fenCode, "g1f3");
        log.debug("evaluationResult: {} {}", evaluationResult.getEvaluation(), evaluationResult.getBestMove());
        
        //Thread.sleep(3000);
        evaluationResult = chessEngineService.getEvaluationResult(fenCode, "c1g5");
        log.debug("evaluationResult: {} {}", evaluationResult.getEvaluation(), evaluationResult.getBestMove());
        
        
        
        
//        // Create a new thread to call requestEvaluationList
//        Runnable evaluationTask = () -> {
//            
//            latch.countDown(); // Decrement the count of the latch, releasing all waiting threads when the count reaches zero
//        };
//
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        executorService.submit(evaluationTask);
//
//        latch.await(); // Wait until the count reaches zero, indicating that the task is complete
//
//        executorService.shutdown();
    }

}
