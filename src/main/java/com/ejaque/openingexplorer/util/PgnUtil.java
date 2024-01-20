package com.ejaque.openingexplorer.util;

import java.io.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PgnUtil {


//    public static String processLine(String line) {
//        // Regular expression to find 'Z0' or 'z0' inside parentheses
//        String regex = "\\([^()]*[Zz]0[^()]*\\)";
//        
//        // Continuously replace occurrences within parentheses
//        while (line.matches(".*" + regex + ".*")) {
//            line = line.replaceAll(regex, "");
//        }
//
//        // Check if there's a 'Z0' or 'z0' not inside parentheses and remove everything after it
//        int index = line.toLowerCase().indexOf("z0");
//        if (index != -1) {
//            line = line.substring(0, index);
//        }
//
//        return line;
//    }

    public static String removeNullMovesFromGame(String pgnLine) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        int z0Depth = Integer.MAX_VALUE;
        int start = -1;
        boolean z0Found = false;
        char currentChar = '\0';
        char prevChar = '\0';
        boolean bracesOpened = false;
        boolean parenthesesOpened = false;
        	
        for (int i = 0; i < pgnLine.length(); i++) {
            currentChar = pgnLine.charAt(i);
            
            // we check if we are opening a bracket (these are pgn comments that can be just added to output without any checks
            if (currentChar == '{') {
            	bracesOpened = true;
            } else if (currentChar == '}') {
            	bracesOpened = false;
            }
            
            // if we keep inside brackets (a pgn comment), keep adding to output without any checks
            // we also check we are in depth above the Z0 depth, because if we are Z0 depth, we do want to process anything)
            if (bracesOpened) {
            	
            	if (depth < z0Depth) {
	            	result.append(currentChar);
	            	continue;
            	
	            // if we are at Z0 depth, we should just ignore and not process this char	
            	} else {
            		continue;
            	}
            }
            

            if (currentChar == '(' && !bracesOpened) {
            	parenthesesOpened = true;
            	if (depth == 0) start = i;
                depth++;
            } else if (prevChar == ')' && !bracesOpened) {
            	parenthesesOpened = false;
            	depth--;
                if (depth == 0) {
                    if (z0Found) {
                        // Reset z0Found and continue without adding to result
                        z0Found = false;
                        z0Depth = Integer.MAX_VALUE;
                        continue;
                    //} else {
                        // If Z0 or z0 was not found, append the whole section
                        //result.append(line.substring(start, i + 1));
                    }
                }
            }

            // Check for "Z0" or "z0"
            if (!z0Found && ((currentChar == 'Z' || currentChar == 'z') && i + 1 < pgnLine.length() && pgnLine.charAt(i + 1) == '0') ) {
                if (depth > 0) {
                    z0Found = true;
                    z0Depth = depth;
                    int lastIndex = result.lastIndexOf("(");
                    if (lastIndex > -1) {
                    	log.info("PGN line has (Z0) section. Line:\n{}", pgnLine);
                    	result.delete(lastIndex, result.length());
                    } else {
                    	log.error("PGN line with no starting parenthesis before Z0\n{}", pgnLine);
                    }
                } else {
                    // Z0 or z0 found outside of any parentheses, return an empty string
                    return "";
                }
            }

            // if we are in the PGN "root" and (we have not found Z0 or above Z0 depth) 
            if (true && (!z0Found || depth < z0Depth)) {
                result.append(currentChar);
                
                // if we detected that we are closing an empty variation like "()"
                // we just delete both parentheses
//                if (prevChar == '(' && currentChar == ')') {
//                	result.delete(result.length() - 2, result.length());
//                }
                
            }
            
            prevChar = currentChar;
        }

        return result.toString();
    }
	
	/**
	 * Process a PGN file to eliminate all variations with Z0 (null moves) or FEN codes (special start positions).
	 * @param inputFilePath
	 * @param outputFilePath
	 */
    public static void processFile(String inputFilePath, String outputFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            
            // to mark that the PGN has some special starting position, we want to IGNORE those
            boolean pgnHasFenPosition = false;
            
            boolean hasEventHeader = false;
            
            while ((line = reader.readLine()) != null) {
                // Skip lines that start with "["
                if (line.startsWith("1")) {  // line with game moves
                	
                	// handle special case when we have no EVENT header
                	if (!hasEventHeader) {
                		writer.write("[Event \"DEFAULT_EVENT_NAME\"]");
                		writer.newLine();
                	}
                	
                	hasEventHeader = false;  // reset this flag as we are ready to process next game
                	
                    if (!pgnHasFenPosition) {
	                	line = removeNullMovesFromGame(line);
	                	writer.newLine();
	                    writer.write(line);
	                    writer.newLine();
	                    writer.newLine();

                    // if we have an special starting position, we IGNORE the game completly
                    } else {
                    	writer.write("");
                    	writer.newLine();
	                    pgnHasFenPosition = false;  // we reset the flag, until next game has FEN header
                    }
                } else if (line.startsWith("[FEN ")) {
                	pgnHasFenPosition = true;
                    writer.write(line);
                    writer.newLine();
                } else if (line.startsWith("[Event ")) {
                	hasEventHeader = true;
                    writer.write(line);
                    writer.newLine();
                } else if (line.startsWith("[")) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Example usage of processFile method
        processFile(
                "C:/Users/eajaquep/Documents/ejp-annotated.pgn", 
        		"C:/Users/eajaquep/Documents/mega-annotated.pgn");        
    }
}
