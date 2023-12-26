OK vs PEND stuff:

PEND: change the output, so it will be an XSLX file with the list of moves and stats

PEND: for each "good move" do an additional check comparing with the stats of 2200 rating section (normal is 2500). We can check for differences in pctg between rating groups (higer level player may choose more often this "good moves").

PEND: Use a PGN with player repertoire for chosing which moves to explore from his side.

PEND: Use a postgres database to save all moves stats and evaluations. Consider also NoSQL, it may be more suited for this.

PEND: Make Chessify evaluate the list of positions (FEN list) and put that in a PGN. Then parse that PGN to add the evaluations of the positions and update moves in database. 