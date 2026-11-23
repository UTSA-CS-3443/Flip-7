package edu.utsa.cs3443.score7.score7core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Flip7CoreTest {

    private Flip7Core game;

    @BeforeEach
    void setUp() {
        game = new Flip7Core();
        game.addPlayer("Alice");
        game.addPlayer("Bob");
    }

    @Test
    void roundDoesNotAdvanceWhenEveryoneIsOut() {
        game.addCard("Alice", Cards.FIVE);
        game.stay("Alice");
        game.bust("Bob");

        assertTrue(game.isRoundComplete());
        assertEquals(1, game.getRound());
        assertEquals(0, game.getPlayer("Alice").getScore());
        assertEquals(5, game.getPlayer("Alice").getRoundScore());
    }

    @Test
    void roundDoesNotAdvanceOnFlip7() {
        for (Cards c : List.of(Cards.ZERO, Cards.ONE, Cards.TWO, Cards.THREE, Cards.FOUR, Cards.FIVE)) {
            assertEquals(CardResult.ADDED, game.addCard("Alice", c));
        }
        assertEquals(CardResult.FLIP_7, game.addCard("Alice", Cards.SIX));

        assertTrue(game.isRoundComplete());
        assertEquals(1, game.getRound());
        // other players can still have their hand recorded
        game.addCard("Bob", Cards.NINE);
        // flip 7 player can't take more number cards, but can still take modifiers
        assertThrows(IllegalStateException.class, () -> game.addCard("Alice", Cards.SEVEN));
        game.addCard("Alice", Cards.PTWO);
    }

    @Test
    void nextRoundBanksScores() {
        // Alice: (3 + 4) * 2 + 4 = 18
        game.addCard("Alice", Cards.THREE);
        game.addCard("Alice", Cards.FOUR);
        game.addCard("Alice", Cards.XTWO);
        game.addCard("Alice", Cards.PFOUR);
        game.stay("Alice");
        // Bob busts with cards in hand -> 0
        game.addCard("Bob", Cards.TWELVE);
        game.bust("Bob");

        game.nextRound();

        assertEquals(2, game.getRound());
        assertEquals(1, game.getRoundsPlayed());
        assertEquals(18, game.getPlayer("Alice").getScore());
        assertEquals(0, game.getPlayer("Bob").getScore());
        assertEquals(List.of(18), game.getPlayer("Alice").getScoreHistory());
        assertTrue(game.getPlayer("Alice").getHand().isEmpty());
        assertEquals(Player.Status.ACTIVE, game.getPlayer("Bob").getStatus());
    }

    @Test
    void nextRoundBanksPlayersStillActive() {
        game.addCard("Alice", Cards.TEN);
        game.nextRound();
        assertEquals(10, game.getPlayer("Alice").getScore());
        assertEquals(0, game.getPlayer("Bob").getScore());
    }

    @Test
    void flip7AddsBonus() {
        for (Cards c : List.of(Cards.ZERO, Cards.ONE, Cards.TWO, Cards.THREE, Cards.FOUR, Cards.FIVE, Cards.SIX)) {
            game.addCard("Alice", c);
        }
        game.nextRound();
        assertEquals(21 + Flip7Core.DEFAULT_FLIP_7_BONUS, game.getPlayer("Alice").getScore());
    }

    @Test
    void removeCardCorrectsHand() {
        game.addCard("Alice", Cards.EIGHT);
        game.addCard("Alice", Cards.PTEN);
        game.removeCard("Alice", Cards.EIGHT);
        assertEquals(List.of(Cards.PTEN), game.getPlayer("Alice").getHand());
        assertThrows(IllegalArgumentException.class, () -> game.removeCard("Alice", Cards.EIGHT));
    }

    @Test
    void reopenUndoesBustAndStay() {
        game.addCard("Alice", Cards.SEVEN);
        game.bust("Alice");
        assertEquals(0, game.getPlayer("Alice").getRoundScore());
        assertThrows(IllegalStateException.class, () -> game.addCard("Alice", Cards.ONE));

        game.reopen("Alice");
        assertEquals(7, game.getPlayer("Alice").getRoundScore());
        game.addCard("Alice", Cards.ONE);

        game.stay("Alice");
        game.reopen("Alice");
        assertEquals(Player.Status.ACTIVE, game.getPlayer("Alice").getStatus());
    }

    @Test
    void duplicateStillBustsAndSecondChanceStillSaves() {
        game.addCard("Alice", Cards.TWO);
        assertEquals(CardResult.BUSTED, game.addCard("Alice", Cards.TWO));
        assertEquals(1, game.getRound());

        game.addCard("Bob", Cards.SECOND_CHANCE);
        game.addCard("Bob", Cards.THREE);
        assertEquals(CardResult.SECOND_CHANCE_USED, game.addCard("Bob", Cards.THREE));
        assertEquals(Player.Status.ACTIVE, game.getPlayer("Bob").getStatus());
    }

    @Test
    void gameEndsOnNextRoundWhenTargetReached() {
        Flip7Core small = new Flip7Core(10);
        small.addPlayer("Alice");
        small.addPlayer("Bob");
        small.addCard("Alice", Cards.TWELVE);
        small.addCard("Bob", Cards.FOUR);
        assertFalse(small.isGameOver());

        small.nextRound();

        assertTrue(small.isGameOver());
        assertEquals("Alice", small.getWinner().orElseThrow().getName());
        assertThrows(IllegalStateException.class, small::nextRound);
    }

    @Test
    void tieAtTopPlaysOn() {
        Flip7Core small = new Flip7Core(10);
        small.addPlayer("Alice");
        small.addPlayer("Bob");
        small.addCard("Alice", Cards.TWELVE);
        small.addCard("Bob", Cards.TWELVE);
        small.nextRound();
        assertFalse(small.isGameOver());
        assertEquals(2, small.getRound());
    }

    @Test
    void endGamePicksLeaderWithoutBankingRound() {
        game.addCard("Alice", Cards.NINE);
        game.nextRound();
        game.addCard("Bob", Cards.TWELVE); // unbanked, must not count

        game.endGame();

        assertTrue(game.isGameOver());
        assertEquals("Alice", game.getWinner().orElseThrow().getName());
        assertEquals(0, game.getPlayer("Bob").getScore());
    }

    @Test
    void endGameWithTieHasNoWinner() {
        game.endGame();
        assertTrue(game.isGameOver());
        assertTrue(game.getWinner().isEmpty());
    }

    @Test
    void cannotAddPlayerMidRound() {
        game.addCard("Alice", Cards.ONE);
        assertThrows(IllegalStateException.class, () -> game.addPlayer("Carol"));
        game.nextRound();
        game.addPlayer("Carol");
    }
}
