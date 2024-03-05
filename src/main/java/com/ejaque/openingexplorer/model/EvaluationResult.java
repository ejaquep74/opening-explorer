package com.ejaque.openingexplorer.model;

import lombok.Getter;

/**
 * Class that represents the results of an engine evaluation of the position.
 */
@Getter
public class EvaluationResult {

	/** Position evaluation. */ 
    private double evaluation;
    
    /** Best move in the position. */
    private String bestMove;
}
