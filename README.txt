
CLASS AnnotatedPgnMarkerServiceIT.testSearchBestMoveIntegration():  generate PGN with all game moves MARKED before analyzed variations 

CLASS PgnUtil:  has method processFile to clear all Z0 (null) moves
*** I suggest running PgnUtil with Xms 2GB and Xmx 4GB, to avoid problems with heap space. With those params, processing 130.000 games divided in 3 files, it was succesful. So files with 45.000 games aprox should be ok


OK vs PEND stuff:

OK: change the output, so it will be an XSLX file with the list of moves and stats


PEND: run chessify in another thread, for evaluation of the good moves, meanwhile the main thread continues exmploring games.

PEND: mark each good move as present or not in chessable, including link with FEN if present

PEND: if any line at any depth comes with sacrifices from chessify, save it, for later analysis by the user

PEND: register sudden changes in evals for a move, might indicate sacrifices or high complexity

PEND: use avg eval for opponent move. For example if 50% is 0.3, 30% -0.8 and 20% is -1.5, the average is 0.5*0.3 - 0.3*0.8 - 0.2*1.5 = -0.39.  For move evals under -3.00 consider eval = -3.00 (big blunders should not have more weight than needed). Also consider a minimum 90% of the games (one you have calculated the average for N moves and total games > 90%, we are done)  

PEND: use chessable tree to check those moves for frequency and engine eval, so we can discover good moves, that are playable and have some human analysis in there. We can even check that analysis to see if after a +0.3 move, the best responses after 6-8 half moves lead to -0.4, so what happend there? maybe is was not an anylisis done deep enough at chessable, maybe we can use that to our advantage maybe it shows the move complexity. Also we can see if the lines analyzed, by playing decent eval moves, have some sacrifices, which makes them more interesting. 

PEND: add EVAL (engine/chessify) of candidate moves (unsual moves that have good stats). check the different lines that engine gives as it goes from depth 15 to 30, save those with sacrifices. save eval of 5 best moves in the position so we can compare with candidates (when a candidate is not in top 5 but has decent eval, it is interesting as it is not attractive but may work). Also check ponderated eval of response moves, up to 80% coverage (for example popularity 50-20-10 so with 3 moves we covered). When the ponderated eval means the move is not easy to respond. We can go deeper 4 more half moves to check this ponderated numbers and draw more conclusions. Also compute the avg total moves considering that 80% coverage. A low average means more "forcing move".

PEND: add EVAL from chessbase's Lets Check. They have a web api we could just scrape (Free plan has lots of info, Premium may have additional moves not sure). These EVALS are already stored in the cloud, but they are only 3. NOTE: could be useful to use NUMBER OF VISITS to the position, as some moves maybe unpopular but with lots of visits which suggest interesting analysis. Also moves analyzed at HIGH DEPTH may indicate some special interest in the variation.

PEND: Compare opponent moves EVALS on avg. Use EVAL at depth 15 vs depth 30 to see how much it changes to estimate complexity, also measure the avg eval of actual moves played (weighted avg) to see on avg how bad is the moves' eval, that may indicate complexity... and you should consider all opponents replies to the line you are suggested for you, based on the opening tree. Sometime you dont have many moves one or to ply ahead but at least you have the eval of the first eval. 

PEND: use the ENGINE EVALS to compare moves found from depth 15 to 30. If the top 5 moves change a lot it might indicate complex position. If onle one or two moves are much better than the rest, it also shows difficulty of finding them (specially if also those moves didn't appear until going deeper).

PEND: add STATS on GOOD MOVES, one I generate excel with stats, I can do a SECOND RUN, looking for games with those moves. Just run equery for ONE MONTH and check all top games and recent games, looking for ANALYZED GAMES, take note of MOVES PLAYED FREQUENTLY BY ONE PLAYER, specially those ANALYZED, and also take note of games with high PRECISION, might indicate lines being practiced. NOTE: one you spot a frequent player, just hit the opening explorer "by player", to check the position and get all games if possible. Also COuNT REPEATED GAMES (same pair of opponents). We cound GENERATE LISt of FREQUENT PLAYERS with "PlayerName: Total Games - Total Analyzed Games" 

PEND: when APP STOP RUNNING, log at the end the total time, time of first error and total errors. If there are few errors at the end, you can keep the results but if not, you better run all again.

PEND: add config for possible Black starting moves, for example:  a7a6, h7h6.  The idea is to put column in output indicating opponent sarting move and the rest the same, only that "probability" of the moves is "assuming that opponent played sartting move X". This would be similar to running the program twice, onece with starting position after a7a6 and then with pos after h7h6.

PEND: for each move in the output, add pgn conducting to that move. It is a text in a "pgn column" of the output excel sheet, so you can search by text on it for sequence of moves.

PEND: for each Good Move, add stats for last year (just call lichess api with another time range)

PEND: for each Good Move, add stats for prominent players (you can get the game ids), important for example is how many above 2700 rating)

PEND: for each Good Move, add stats for analyzed games, cloud eval, maybe heavy analyzed lines means someone is analyzing this heavily.

PEND: for each Good Move, mark it sacrifice if the move itself is a sacrifice or if after it we can have a sacrifice (for example in the best line from the engine, or in some of the example games, when the sacrifice few moves later has good eval) 

PEND: add in the Excel a Mark "ERROR" with number of errors so we know if we have PROBLEMS with throttling and Lichess (we didnt explore all the possible moves)

PEND: add lichess cloud eval for moves (use lichess api)

PEND: for each "good move" do an additional check comparing with the stats of 2200 rating section (normal is 2500). We can check for differences in pctg between rating groups (higer level player may choose more often this "good moves").

PEND: to estimate probability of a position, you have to sum the probability of all transpositions, that's combinations of move leading to the same FEN code (maybe just save the sequence of opponent's moves, order them alphabetically, and then group)

PEND: connect to chessbase "api" (does not exist but could be scraped) to get info like moves' stats and cpu eval stats for all moves.

PEND: EloUtil.getPerformance(double, double) FIX Rating Performance: calculate the opponent's rating avg of a move by exploring one move deeper and taking weighted avg of all avg rating for each reply. With this rtg avg for each color, and the info on W/D/L you can calculate the rating performance.

PEND: Use a PGN with player repertoire for choosing which moves to explore from his side.

PEND: Use a postgres database to save all moves stats and evaluations. Consider also NoSQL, it may be more suited for this.

PEND: Make Chessify evaluate the list of positions (FEN list) and put that in a PGN. Then parse that PGN to add the evaluations of the positions and update moves in database. 