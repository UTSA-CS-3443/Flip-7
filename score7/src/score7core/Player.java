package score7core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only view of a player for library users. All mutation goes through
 * {@link Flip7Core}, which is why the mutators are package-private.
 */
public class Player {

    public enum Status { ACTIVE, STAYED, BUSTED }

    private final String name;
    private final int flip7Size;
    private final int flip7Bonus;
    private int score;
    private Status status = Status.ACTIVE;

    private final List<Cards> hand = new ArrayList<>();
    private final List<Integer> scoreHistory = new ArrayList<>();
    private final List<List<Cards>> cardHistory = new ArrayList<>();

    Player(String name, int flip7Size, int flip7Bonus) {
        this.name = name;
        this.flip7Size = flip7Size;
        this.flip7Bonus = flip7Bonus;
    }

    // ---- public read-only API ----

    public String getName() {
        return name;
    }

    /** Total banked score across all finished rounds. */
    public int getScore() {
        return score;
    }

    public Status getStatus() {
        return status;
    }

    /** Cards in the current round. */
    public List<Cards> getHand() {
        return Collections.unmodifiableList(hand);
    }

    /** Points earned in each finished round, oldest first. */
    public List<Integer> getScoreHistory() {
        return Collections.unmodifiableList(scoreHistory);
    }

    /** Hand of each finished round, oldest first (includes the card that caused a bust). */
    public List<List<Cards>> getCardHistory() {
        return Collections.unmodifiableList(cardHistory);
    }

    /**
     * What this player would score if the round ended now: 0 if busted,
     * otherwise number total, doubled by XTWO, plus P bonuses, plus the Flip 7 bonus.
     */
    public int getRoundScore() {
        if (status == Status.BUSTED) {
            return 0;
        }
        int total = 0;
        boolean doubled = false;
        int bonus = 0;
        for (Cards c : hand) {
            switch (c.getKind()) {
                case NUMBER:     total += c.getValue(); break;
                case MULTIPLIER: doubled = true; break;
                case BONUS:      bonus += c.getValue(); break;
                default:         break; // action cards do not affect score
            }
        }
        if (doubled) {
            total *= 2;
        }
        total += bonus;
        if (hasFlip7()) {
            total += flip7Bonus;
        }
        return total;
    }

    // ---- package-private mutators used by Flip7Core ----

    boolean hasCard(Cards card) {
        return hand.contains(card);
    }

    boolean hasFlip7() {
        Set<Cards> unique = EnumSet.noneOf(Cards.class);
        for (Cards c : hand) {
            if (c.isNumber()) {
                unique.add(c);
            }
        }
        return unique.size() >= flip7Size;
    }

    void addToHand(Cards card) {
        hand.add(card);
    }

    /** Removes a SECOND_CHANCE from the hand if there is one. */
    boolean consumeSecondChance() {
        return hand.remove(Cards.SECOND_CHANCE);
    }

    void stay() {
        status = Status.STAYED;
    }

    void bust() {
        status = Status.BUSTED;
    }

    boolean isActive() {
        return status == Status.ACTIVE;
    }

    boolean hasActivity() {
        return !hand.isEmpty() || status != Status.ACTIVE;
    }

    /** Banks the round score, records history, and resets for the next round. */
    void finishRound() {
        int earned = getRoundScore();
        score += earned;
        scoreHistory.add(earned);
        cardHistory.add(new ArrayList<>(hand));
        hand.clear();
        status = Status.ACTIVE;
    }
}
