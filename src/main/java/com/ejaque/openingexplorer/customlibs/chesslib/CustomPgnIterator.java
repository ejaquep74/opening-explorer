package com.ejaque.openingexplorer.customlibs.chesslib;

import java.util.Iterator;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.pgn.GameLoader;
import com.github.bhlangonijr.chesslib.util.LargeFile;

import lombok.extern.slf4j.Slf4j;

/**
 * The type Pgn Iterator. This is a CUSTOM modification to allow ignoring exception when processing games in a file.
 * <p>
 * The pgn iterator permits iterating over large PGN files without piling up every game in the memory
 */
@Slf4j
public class CustomPgnIterator implements Iterable<Game>, AutoCloseable {

    private final Iterator<String> pgnLines;

    private Game game;
    
    int gameCount;

    /**
     * Instantiates a new Pgn holder.
     *
     * @param filename the filename
     * @throws Exception reading the file
     */
    public CustomPgnIterator(String filename) throws Exception {

        this(new LargeFile(filename));
    }

    public CustomPgnIterator(LargeFile file) {

        this.pgnLines = file.iterator();
        loadNextGame();
    }

    public CustomPgnIterator(Iterable<String> pgnLines) {

        this.pgnLines = pgnLines.iterator();
        loadNextGame();
    }

    public CustomPgnIterator(Iterator<String> pgnLines) {

        this.pgnLines = pgnLines;
        loadNextGame();
    }

    @Override
    public Iterator<Game> iterator() {
        return new GameIterator();
    }

    @Override
    public void close() throws Exception {

        if (pgnLines instanceof LargeFile) {
            ((LargeFile) (pgnLines)).close();
        }
    }

    private void loadNextGame() {

        game = GameLoader.loadNextGame(pgnLines);
    }

    private class GameIterator implements Iterator<Game> {

        public boolean hasNext() {

            return game != null;
        }

        public Game next() {
            Game current = game;
            try {
                loadNextGame();
                gameCount++;
                log.debug("GAME COUNT: {}", gameCount);
            } catch (Exception e) {
                log.error("Error loading next game. IGNORING ERROR.", e);
                // Skip to the next game after encountering an error
                if (hasNext()) {
                    return next();
                }
            }
            return current;
        }
        
        public void remove() {
        }
    }
}