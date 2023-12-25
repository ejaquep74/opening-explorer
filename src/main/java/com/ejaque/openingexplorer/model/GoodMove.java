package com.ejaque.openingexplorer.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class GoodMove {

	private String fen;
	private String move;
	private double probabilityOcurring;
	private int ratingRank;
	private double ratingPercentile;
	private double averageRating;
	private double averageRatingForAllMoves;
}
