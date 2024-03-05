package com.ejaque.openingexplorer.model;

import lombok.Getter;


@Getter
public class EvaluationTask {

	/** FEN code for the position to evaluate. */ 
    private String fenCode;
    
    public EvaluationTask(String fenCode) {
    	this.fenCode = fenCode; 
	}
    
    // Constructor, getters, setters
}
