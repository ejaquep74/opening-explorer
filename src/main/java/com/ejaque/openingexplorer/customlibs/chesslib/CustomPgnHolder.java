package com.ejaque.openingexplorer.customlibs.chesslib;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.bhlangonijr.chesslib.game.Event;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.Player;
import com.github.bhlangonijr.chesslib.game.Round;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import com.github.bhlangonijr.chesslib.pgn.PgnLoadListener;
import com.github.bhlangonijr.chesslib.util.LargeFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom implementation of {@link PgnHolder}, the idea is to allow here ignore
 * exception when processing games (it will only log the error).
 */
@Slf4j
public class CustomPgnHolder {

    private final Map<String, Event> event = new HashMap<String, Event>();
    private final Map<String, Player> player = new HashMap<String, Player>();
    private final List<Game> games = new ArrayList<Game>();
    private final List<PgnLoadListener> listener = new ArrayList<PgnLoadListener>();
    private String fileName;
    private Integer size;
    private boolean lazyLoad;

    /**
     * Instantiates a new Pgn holder.
     *
     * @param filename the filename
     */
    public CustomPgnHolder(String filename) {
        setFileName(filename);
        setLazyLoad(false);
    }

    /**
     * Clean up.
     */
    public void cleanUp() {
        event.clear();
        player.clear();
        games.clear();
        listener.clear();
        size = 0;
    }

    /**
     * Gets file name.
     *
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets file name.
     *
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Gets event.
     *
     * @return the event
     */
    public Map<String, Event> getEvent() {
        return event;
    }

    /**
     * Gets player.
     *
     * @return the player
     */
    public Map<String, Player> getPlayer() {
        return player;
    }

    /**
     * Get list of games.
     *
     * @return the game
     */
    public List<Game> getGames() {
        return games;
    }

    /**
     * Gets game.
     *
     * @return the game
     * @deprecated fixed typo - use {@link #getGames()} instead
     */
    public List<Game> getGame() {
        return games;
    }

    /**
     * Load the PGN file
     *
     * @throws Exception the exception
     */
    public void loadPgn() throws Exception {
        loadPgn(new LargeFile(getFileName()));
    }

    /**
     * Count games in PGN file.
     * For this all lines in the PGN file are counted, which start with the string "[Event "
     * because this field is mandatory by PGN definition.
     *
     * @return number of games in PGN file
     * @throws IOException if PGN file set via constructor was not found
     */
    public long countGamesInPgnFile() throws IOException {
        return Files.lines(Paths.get(this.fileName))
                .filter(s -> s.startsWith("[Event "))
                .count();
    }

    /**
     * Load a PGN file
     *
     * @param file the file to be loaded
     * @throws Exception the exception
     */
    public void loadPgn(LargeFile file) throws Exception {

        size = 0;

        CustomPgnIterator games = new CustomPgnIterator(file);

        try {
            for (Game game : games) {
            	try {
            		addGame(game);
            	} catch (Exception e) {
            		log.error("Error processing Game. IGNORING ERROR.", e);
            	}
            }
        } finally {
            file.close();
        }
    }

    /**
     * Load a PGN from a string
     *
     * @param pgn string to be loaded
     */
    public void loadPgn(String pgn) {

        Iterable<String> iterable = Arrays.asList(pgn.split("\n"));
        CustomPgnIterator games = new CustomPgnIterator(iterable.iterator());
        for (Game game : games) {
            addGame(game);
        }
    }

    /**
     * Save the PGN
     */
    public void savePgn() {

        try {
            FileWriter outFile = new FileWriter(getFileName());
            PrintWriter out = new PrintWriter(outFile);
            for (Event event : getEvent().values()) {
                for (Round round : event.getRound().values()) {
                    for (Game game : round.getGame()) {
                        if (game != null) {
                            out.println();
                            out.print(game);
                            out.println();
                        }
                    }
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Gets size.
     *
     * @return the size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Is lazy load boolean.
     *
     * @return the lazyLoad
     */
    public boolean isLazyLoad() {
        return lazyLoad;
    }

    /**
     * Sets lazy load.
     *
     * @param lazyLoad the lazyLoad to set
     */
    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    /**
     * Gets listener.
     *
     * @return the listener
     */
    public List<PgnLoadListener> getListener() {
        return listener;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Event event : getEvent().values()) {
            for (Round round : event.getRound().values()) {
                for (Game game : round.getGame()) {
                    if (game != null) {
                        sb.append('\n');
                        sb.append(game);
                        sb.append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    private void addGame(Game game) {

        Event event = getEvent().get(game.getRound().getEvent().getName());
        if (event == null) {
            getEvent().put(game.getRound().getEvent().getName(), game.getRound().getEvent());
        }
        Player whitePlayer = getPlayer().get(game.getWhitePlayer().getId());
        if (whitePlayer == null) {
            getPlayer().put(game.getWhitePlayer().getId(), game.getWhitePlayer());
        }
        Player blackPlayer = getPlayer().get(game.getBlackPlayer().getId());
        if (blackPlayer == null) {
            getPlayer().put(game.getBlackPlayer().getId(), game.getBlackPlayer());
        }
        this.games.add(game);

        // Notify all registered Listener about added game
        this.getListener().forEach(pgnLoadListener -> pgnLoadListener.notifyProgress(this.games.size()));
    }

}
