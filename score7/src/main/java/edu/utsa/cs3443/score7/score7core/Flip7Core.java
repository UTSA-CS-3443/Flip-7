package edu.utsa.cs3443.score7.score7core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Score tracker / state machine for Flip 7. It does not play the game; the caller
 * reports what happened and this class keeps score and rejects illegal moves.
 *
 * Rounds never advance on their own. Players stay, bust or reach Flip 7, but nothing
 * is banked until the caller invokes {@link #nextRound()}. Until then a player's hand
 * can be corrected with {@link #removeCard} and a stay/bust can be undone with
 * {@link #reopen}. This keeps a mistaken entry from being banked.
 *
 * The game ends after {@link #nextRound()} when at least one player has reached the
 * target score and a single player holds the highest score (a tie at the top plays on),
 * or immediately when {@link #endGame()} is called.
 */
public class Flip7Core {

    public static final int DEFAULT_TARGET_SCORE = 200;
    public static final int DEFAULT_FLIP_7_SIZE = 7;
    public static final int DEFAULT_FLIP_7_BONUS = 15;

    private final List<Player> players = new ArrayList<>();
    private final int targetScore;
    private final int flip7Size;
    private final int flip7Bonus;
    private int round = 1;
    private Player winner;
    private boolean gameOver;

    public Flip7Core() {
        this(DEFAULT_TARGET_SCORE);
    }

    public Flip7Core(int targetScore) {
        this(targetScore, DEFAULT_FLIP_7_SIZE, DEFAULT_FLIP_7_BONUS);
    }

    /**
     * @param targetScore score that ends the game
     * @param flip7Size   unique number cards needed for a Flip 7 (1 to 13, since ZERO..TWELVE are the only number cards)
     * @param flip7Bonus  points awarded for a Flip 7
     */
    public Flip7Core(int targetScore, int flip7Size, int flip7Bonus) {
        if (targetScore <= 0) {
            throw new IllegalArgumentException("Target score must be positive");
        }
        if (flip7Size < 1 || flip7Size > 13) {
            throw new IllegalArgumentException("Flip 7 size must be between 1 and 13");
        }
        if (flip7Bonus < 0) {
            throw new IllegalArgumentException("Flip 7 bonus must not be negative");
        }
        this.targetScore = targetScore;
        this.flip7Size = flip7Size;
        this.flip7Bonus = flip7Bonus;
    }

    // ---- setup ----

    /** Players may only join between rounds, never while a round has activity. */
    public void addPlayer(String name) {
        requireGameInProgress();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        if (findPlayer(name) != null) {
            throw new IllegalArgumentException("Player already exists: " + name);
        }
        if (roundHasActivity()) {
            throw new IllegalStateException("Cannot add players in the middle of a round");
        }
        players.add(new Player(name, flip7Size, flip7Bonus));
    }

    // ---- moves ----

    /**
     * Records a card for a player. A duplicate number card busts the player unless
     * they hold a SECOND_CHANCE, which is consumed instead. Reaching Flip 7 does not
     * end the round; the player simply cannot take more number cards.
     */
    public CardResult addCard(String name, Cards card) {
        if (card == null) {
            throw new IllegalArgumentException("Card must not be null");
        }
        Player p = activePlayer(name);

        if (card.isNumber() && p.hasFlip7()) {
            throw new IllegalStateException(name + " already has a Flip 7");
        }

        if (card.isNumber() && p.hasCard(card)) {
            if (p.consumeSecondChance()) {
                return CardResult.SECOND_CHANCE_USED;
            }
            p.addToHand(card); // kept so the history shows what caused the bust
            p.bust();
            return CardResult.BUSTED;
        }

        p.addToHand(card);

        if (p.hasFlip7()) {
            return CardResult.FLIP_7;
        }
        if (card == Cards.FREEZE) {
            p.stay();
            return CardResult.FROZEN;
        }
        return CardResult.ADDED;
    }

    /** Removes one copy of a card from an active player's hand (corrects a mistaken entry). */
    public void removeCard(String name, Cards card) {
        if (card == null) {
            throw new IllegalArgumentException("Card must not be null");
        }
        Player p = activePlayer(name);
        if (!p.removeFromHand(card)) {
            throw new IllegalArgumentException(name + " does not have " + card);
        }
    }

    /** Player banks their hand and takes no more cards this round. */
    public void stay(String name) {
        activePlayer(name).stay();
    }

    /** Player busts: their round score is 0. This is the primary way busts are reported. */
    public void bust(String name) {
        activePlayer(name).bust();
    }

    /** Undoes a stay or bust so the player is active again for the current round. */
    public void reopen(String name) {
        requireGameInProgress();
        getPlayer(name).reopen();
    }

    public void action(String name, Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action must not be null");
        }
        switch (action) {
            case STAY: stay(name); break;
            case BUST: bust(name); break;
        }
    }

    /**
     * Banks the current round for every player (players still active bank whatever
     * they hold, busted players score 0), then either ends the game or starts the
     * next round. This is the only way a round advances.
     */
    public void nextRound() {
        requireGameInProgress();
        if (players.isEmpty()) {
            throw new IllegalStateException("No players");
        }
        for (Player p : players) {
            p.finishRound();
        }
        winner = findWinner(true);
        if (winner != null) {
            gameOver = true;
        } else {
            round++;
        }
    }

    /**
     * Ends the game immediately without banking the current round. The winner is
     * the player with the highest score; if the top score is tied there is no winner.
     */
    public void endGame() {
        requireGameInProgress();
        winner = findWinner(false);
        gameOver = true;
    }

    // ---- queries ----

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getPlayer(String name) {
        Player p = findPlayer(name);
        if (p == null) {
            throw new IllegalArgumentException("No such player: " + name);
        }
        return p;
    }

    /** Current round, starting at 1. Does not advance after the game ends. */
    public int getRound() {
        return round;
    }

    /** Number of rounds that have been banked. */
    public int getRoundsPlayed() {
        return players.isEmpty() ? 0 : players.get(0).getScoreHistory().size();
    }

    /** True when no player is still active or someone has a Flip 7. Informational only. */
    public boolean isRoundComplete() {
        boolean anyActive = false;
        for (Player p : players) {
            if (p.hasFlip7()) {
                return true;
            }
            if (p.isActive()) {
                anyActive = true;
            }
        }
        return !anyActive;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public int getFlip7Size() {
        return flip7Size;
    }

    public int getFlip7Bonus() {
        return flip7Bonus;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** The winner, or empty while the game is running or if it ended in a tie. */
    public Optional<Player> getWinner() {
        return Optional.ofNullable(winner);
    }

    // ---- internals ----

    private Player findPlayer(String name) {
        for (Player p : players) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private Player activePlayer(String name) {
        requireGameInProgress();
        Player p = getPlayer(name);
        if (!p.isActive()) {
            throw new IllegalStateException(name + " is already out of this round (" + p.getStatus() + ")");
        }
        return p;
    }

    private void requireGameInProgress() {
        if (isGameOver()) {
            throw new IllegalStateException("The game is over");
        }
    }

    private boolean roundHasActivity() {
        for (Player p : players) {
            if (p.hasActivity()) {
                return true;
            }
        }
        return false;
    }

    /** Sole leader, or null on a tie at the top or (if required) below the target. */
    private Player findWinner(boolean requireTarget) {
        int best = Integer.MIN_VALUE;
        Player leader = null;
        boolean tied = false;
        for (Player p : players) {
            if (p.getScore() > best) {
                best = p.getScore();
                leader = p;
                tied = false;
            } else if (p.getScore() == best) {
                tied = true;
            }
        }
        if (tied || (requireTarget && best < targetScore)) {
            return null;
        }
        return leader;
    }
}
