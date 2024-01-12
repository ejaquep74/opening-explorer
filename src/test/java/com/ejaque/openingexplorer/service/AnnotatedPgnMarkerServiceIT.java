package com.ejaque.openingexplorer.service;



import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class AnnotatedPgnMarkerServiceIT {

    @Autowired
    private AnnotatedPgnMarkerService annotatedPgnMarkerService;

    @Test
    public void testSearchBestMoveIntegration() throws Exception {
        try {
            annotatedPgnMarkerService.saveAnnotatedPgnToFile(
            		"C:/Users/eajaquep/Documents/chesscom-gotd.pgn", 
            		"C:/Users/eajaquep/Documents/chesscom-gotd-MARKED.pgn");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
