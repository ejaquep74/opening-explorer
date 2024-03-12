package com.ejaque.openingexplorer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class UciUtilTest {


    @Test
    public void testDetectSacrifices() {
    	
    	// from this nice example: https://www.chess.com/article/view/the-positional-queen-sacrifice
//        String fen = "b1r5/2r1kp2/3p1p1p/1pq1pP1N/4PbPP/pPPR1Q2/P1B5/1K1R4 b - - 1 31";
//        List<String> moves = Arrays.asList("c5c3", "d3c3", "c7c3", "f3e2");
//
//        int sacrificesDetected = UciUtil.detectSacrifices(fen, moves).size();
//
//        assertEquals(1, sacrificesDetected, "Sacrifice should be detected in the sequence of moves.");
    }

}
