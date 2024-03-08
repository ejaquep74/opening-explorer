package com.ejaque.openingexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.ejaque.openingexplorer.service.OpeningExplorerService;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        // Start the Spring application and obtain the context
        ConfigurableApplicationContext context = SpringApplication.run(MainApplication.class, args);

        // Get the OpeningExplorerService bean from the application context
        OpeningExplorerService openingExplorer = context.getBean(OpeningExplorerService.class);

        // call main methods
        try {
            openingExplorer.startSearch();
            openingExplorer.exportGoodMoves();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
