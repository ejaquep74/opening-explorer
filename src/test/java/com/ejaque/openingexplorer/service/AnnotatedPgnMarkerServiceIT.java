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
            String annotatedPgn = annotatedPgnMarkerService.markImportantMovesInPgn("C:/Users/eajaquep/Documents/tal-korchnoi.pgn");
            System.out.println(annotatedPgn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
