package com.ejaque.openingexplorer.event;

import java.util.concurrent.CompletableFuture;

import org.springframework.context.ApplicationEvent;

import com.ejaque.openingexplorer.service.ChessEngineService;

import lombok.Getter;

/**
 * EVent to be generated in {@link ChessEngineService}, currently NOT USED as we
 * prefer {@link CompletableFuture} to wait for results of that single threaded
 * class.
 */
@Getter
public class EvaluationResultEvent extends ApplicationEvent {
    
	private static final long serialVersionUID = 1L;
	
	private double evaluation;
	private String fenCode;

	
    public EvaluationResultEvent(Object source, String fenCode, double evaluation) {
        super(source);
        this.fenCode = fenCode;
        this.evaluation = evaluation;
    }
}