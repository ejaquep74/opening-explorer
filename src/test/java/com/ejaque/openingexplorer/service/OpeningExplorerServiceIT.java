package com.ejaque.openingexplorer.service;



import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class OpeningExplorerServiceIT {

    @Autowired
    private OpeningExplorerService openingExplorer;

    @Test
    public void testSearchBestMoveIntegration() throws Exception {
        openingExplorer.startSearch();
        openingExplorer.printBestMoves();        
    }
}
