package com.ejaque.openingexplorer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.bhlangonijr.chesslib.move.MoveException;

public class PgnUtilTest {

    @Test
    public void testProcessEmptyLine() {
        String input = "";
        String expected = "";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessSimpleLine() {
        String input = "xxxx";
        String expected = "xxxx";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithNestedParentheses() {
        String input = "bla bla (xxx z0 (jlsjd) jdsk)";
        String expected = "bla bla ";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithNestedParentheses2() {
        String input = "bla z0 bla (xxx z0 (jlsjd) jdsk)";
        String expected = "";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithParenthesesAndBracelets1() {
        String input = "bla bla {xxx (hmm)} (xxx z0 (jlsjd) jdsk)";
        String expected = "bla bla {xxx (hmm)} ";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithParenthesesAndBracelets2() {
        String input = "bla bla {xxx (hmm)} (xxx {aaa (bbb)} (jlsjd z0 {ddd}) jdsk)";
        String expected = "bla bla {xxx (hmm)} (xxx {aaa (bbb)}  jdsk)";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithParenthesesAndBracelets3() {
        String input = "bla bla {xxx (hmm)} (xxx {aaa (bbb) z0} (jlsjd z0 {ddd}) jdsk)";
        String expected = "bla bla {xxx (hmm)} (xxx {aaa (bbb) z0}  jdsk)";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithParenthesesAndBracelets4() {
        String input = "bla bla (ddd) (z0 sdf z0)";
        String expected = "bla bla (ddd) ";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithParenthesesAndBracelets5() {
        String input = "bla bla (xxx z0) ({aaa} ddd ({bbb} sdf {ccc}))";
        String expected = "bla bla ({aaa} ddd ({bbb} sdf {ccc}))";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }
    
    @Test
    public void testProcessLineWithDeeplyNestedParentheses() {
        String input = "bla bla (xxx (jlsjd (z0 sdshk)) jdsk)";
        String expected = "bla bla (xxx (jlsjd ) jdsk)";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    @Test
    public void testProcessLineWithfullPGN1() {
        String input = "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4 Bc5 5. Be3 Qf6 6. c3 Nge7 7. Bc4 d6 8. O-O Bxd4 {  Dieser Abtausch führt mancherlei Vorteile mit sich, jedoch einzig und allein für den Gegner. Das Zentrum wird verstärkt, der Damenspringer kann sich günstig entwickeln, die Türme finden gute Angriffslinie, und der schwarze Springer wird von e5 abgehalten. Das dürfte ausreichen, um den Tausch als gänzlich verfehlt erscheinen zu lassen.} 9. cxd4 Qg6 10. Nc3 O-O 11. f4 Bg4 12. Qd2 Na5 13. Bd3 f5 14. e5 d5 15. Qf2 Nac6 16. Rac1 a6 17. Na4 b6 18. Rc3 Qe8 19. b3 Qd7 20. Rfc1 Bh5 21. Qf1 Ra7 22. a3 Nb8 23. R1c2 Qd8 24. Qc1 c6 25. Bf2 Nc8 26. Bf1 Bf7 27. Nb2 Be6 28. Nd3 Qe8 29. Be2 Qe7 ({Hält Schwarz statt dessen durch} 29... a5 {den Springer vom Felde b4 ab, so eröffnet Weiß mit} 30. Bh4 Z0 31. Nf2 Z0 32. Qd1 Z0 {und} 33. Rg3 {den Angriff gegen den Königsflügel.}) 30. Nb4 Bd7 31. Bf3 Qf7 32. Bh4 Ne7 33. Re2 {} Qe6 ({Hier war} 33... Ng6 {der einzig richtige Zug.}) {Es folgt nun eine ebenso elegante wie entscheidende Opferkombination.} 34. Bxe7 Qxe7 35. e6 Be8 ({Wird der Bauer geschlagen} 35... Bxe6 {, so entscheidet sowol} 36. Nxc6 ({als} 36. Rxc6 {sehr schnell zu Gunsten des Anziehenden.})) 36. Bxd5 cxd5 37. Nxd5 Qd6 38. e7 Nc6 {Der Turm muss ganz still halten.} ({Ginge er, dem Angriff des Bauern ausweichend, nach} 38... Rf7 {, so folgte einfach} 39. Rc8 Qd7 40. Nxb6 Qb5 41. a4) ({Wenn nach} 38... Rf6 {, so ähnlich} 39. Nxf6+ Qxf6 40. Rc8 Qf7 41. Qc4 $1) ({Nimmt Schwarz schließlich den Springer} 38... Qxd5 {, so entscheidet nach} 39. exf8=Q+ Kxf8 {gleichfalls} 40. Rc8) 39. exf8=Q+ Kxf8 40. Ne3 Nxd4 41. Rd2 Qxf4 42. Qf1 Qe5 43. Rcd3 Re7 44. Nc4 Ne2+ 45. Kh1 Qf4 46. Rf3 Qe4 47. Rxf5+ Rf7 48. Rxf7+ Bxf7 49. Rxe2 1-0";
        String expected = "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4 Bc5 5. Be3 Qf6 6. c3 Nge7 7. Bc4 d6 8. O-O Bxd4 {  Dieser Abtausch führt mancherlei Vorteile mit sich, jedoch einzig und allein für den Gegner. Das Zentrum wird verstärkt, der Damenspringer kann sich günstig entwickeln, die Türme finden gute Angriffslinie, und der schwarze Springer wird von e5 abgehalten. Das dürfte ausreichen, um den Tausch als gänzlich verfehlt erscheinen zu lassen.} 9. cxd4 Qg6 10. Nc3 O-O 11. f4 Bg4 12. Qd2 Na5 13. Bd3 f5 14. e5 d5 15. Qf2 Nac6 16. Rac1 a6 17. Na4 b6 18. Rc3 Qe8 19. b3 Qd7 20. Rfc1 Bh5 21. Qf1 Ra7 22. a3 Nb8 23. R1c2 Qd8 24. Qc1 c6 25. Bf2 Nc8 26. Bf1 Bf7 27. Nb2 Be6 28. Nd3 Qe8 29. Be2 Qe7 30. Nb4 Bd7 31. Bf3 Qf7 32. Bh4 Ne7 33. Re2 {} Qe6 ({Hier war} 33... Ng6 {der einzig richtige Zug.}) {Es folgt nun eine ebenso elegante wie entscheidende Opferkombination.} 34. Bxe7 Qxe7 35. e6 Be8 ({Wird der Bauer geschlagen} 35... Bxe6 {, so entscheidet sowol} 36. Nxc6 ({als} 36. Rxc6 {sehr schnell zu Gunsten des Anziehenden.})) 36. Bxd5 cxd5 37. Nxd5 Qd6 38. e7 Nc6 {Der Turm muss ganz still halten.} ({Ginge er, dem Angriff des Bauern ausweichend, nach} 38... Rf7 {, so folgte einfach} 39. Rc8 Qd7 40. Nxb6 Qb5 41. a4) ({Wenn nach} 38... Rf6 {, so ähnlich} 39. Nxf6+ Qxf6 40. Rc8 Qf7 41. Qc4 $1) ({Nimmt Schwarz schließlich den Springer} 38... Qxd5 {, so entscheidet nach} 39. exf8=Q+ Kxf8 {gleichfalls} 40. Rc8) 39. exf8=Q+ Kxf8 40. Ne3 Nxd4 41. Rd2 Qxf4 42. Qf1 Qe5 43. Rcd3 Re7 44. Nc4 Ne2+ 45. Kh1 Qf4 46. Rf3 Qe4 47. Rxf5+ Rf7 48. Rxf7+ Bxf7 49. Rxe2 1-0";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }

    


    @Test
    public void testProcessLineWithZ0OutsideParentheses() {
        String input = "1. e4 z0";
        String expected = "";
        assertEquals(expected, PgnUtil.removeNullMovesFromGame(input));
    }
    
    
    @Test
    public void testGetPgn() throws MoveException {
    	
    	// from this nice example: https://www.chess.com/article/view/the-positional-queen-sacrifice
        String fen = "b1r5/2r1kp2/3p1p1p/1pq1pP1N/4PbPP/pPPR1Q2/P1B5/1K1R4 b - - 1 31";
        List<String> uciMoves = Arrays.asList("c5c3", "d3c3", "c7c3", "f3e2");
        
        String pgn = PgnUtil.getPgn(fen, uciMoves);
        assertNotNull(pgn);

        // You might want to print or further assert something with the PGN to ensure correctness
        System.out.println(pgn);
    }
}

