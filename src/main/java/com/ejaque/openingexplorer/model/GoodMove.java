package com.ejaque.openingexplorer.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a good chess move identified during move analysis. This class encapsulates 
 * various metrics and characteristics of a chess move that are used to determine its efficacy 
 * in a given chess position.
 * 
 * <p>This class includes metrics such as probability of occurrence, rating rank, rating percentile, 
 * average rating of the move, and average rating for all moves in a given position.</p>
 * 
 * <p>Attributes:</p>
 * <ul>
 *     <li>{@code fen} - The FEN (Forsyth-Edwards Notation) string representing the chess board position for this move.</li>
 *     <li>{@code move} - The move in UCI (Universal Chess Interface) format.</li>
 *     <li>{@code probabilityOcurring} - The probability of this move occurring in the given board position.</li>
 *     <li>{@code ratingRank} - The rank of this move based on the average rating of players who played it.</li>
 *     <li>{@code ratingPercentile} - The percentile standing of this move in terms of player ratings.</li>
 *     <li>{@code averageRating} - The average rating of players who have played this move.</li>
 *     <li>{@code averageRatingForAllMoves} - The average rating for all moves in the given board position.</li>
 * </ul>
 *
 * <p>Utilizes Lombok annotations for boilerplate code like getters, setters, builder, and toString method.</p>
 */
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
