
CLASS AnnotatedPgnMarkerServiceIT.testSearchBestMoveIntegration():  generate PGN with all game moves MARKED before analyzed variations 

CLASS PgnUtil:  has method processFile to clear all Z0 (null) moves
*** I suggest running PgnUtil with Xms 2GB and Xmx 4GB, to avoid problems with heap space. With those params, processing 130.000 games divided in 3 files, it was succesful. So files with 45.000 games aprox should be ok


OK vs PEND stuff:

OK: change the output, so it will be an XSLX file with the list of moves and stats

PEND: add lichess cloud eval for moves (use lichess api)

PEND: for each "good move" do an additional check comparing with the stats of 2200 rating section (normal is 2500). We can check for differences in pctg between rating groups (higer level player may choose more often this "good moves").

PEND: to estimate probability of a position, you have to sum the probability of all transpositions, that's combinations of move leading to the same FEN code (maybe just save the sequence of opponent's moves, order them alphabetically, and then group)

PEND: connect to chessbase "api" (does not exist but could be scraped) to get info like moves' stats and cpu eval stats for all moves.

PEND: EloUtil.getPerformance(double, double) FIX Rating Performance: calculate the opponent's rating avg of a move by exploring one move deeper and taking weighted avg of all avg rating for each reply. With this rtg avg for each color, and the info on W/D/L you can calculate the rating performance.

PEND: Use a PGN with player repertoire for choosing which moves to explore from his side.

PEND: Use a postgres database to save all moves stats and evaluations. Consider also NoSQL, it may be more suited for this.

PEND: Make Chessify evaluate the list of positions (FEN list) and put that in a PGN. Then parse that PGN to add the evaluations of the positions and update moves in database. 