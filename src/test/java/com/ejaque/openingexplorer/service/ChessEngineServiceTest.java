package com.ejaque.openingexplorer.service;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    	chessEngineService.startWSSConnection();
    	chessEngineService.sendInitCommands("rnbqkbnr/ppppppp1/7p/8/6P1/7P/PPPPPP2/RNBQKBNR b KQkq - 0 2");
    	
//    	// DELETE, checking these commands for now...
//    	chessEngineService.sendCommand("position fen r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP1BPPBP/RN1Q1RK1 w - - 8 8");
//    	chessEngineService.sendCommand("go depth 25");
    }    

    
    @Test
    public void testRequestEvaluationList() throws InterruptedException {
        String fenCode = "r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PPQBPPBP/RN1Q1RK1 w - - 8 8";
        var moves = Arrays.asList("d1c2", "d1b3"); 
        CountDownLatch latch = new CountDownLatch(1);
        
        chessEngineService.requestEvaluationList(fenCode, moves, 0);

        EvaluationResult evaluationResult = chessEngineService.getEvaluationResult(fenCode);
        
        log.debug("evaluationResult: {}", evaluationResult);
        
        
        
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
