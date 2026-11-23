package edu.utsa.cs3443.score7.model;

import edu.utsa.cs3443.score7.score7core.Flip7Core;
import edu.utsa.cs3443.score7.score7core.Player;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A finished game as stored in history.
 *
 * @param playedAt when the game finished (second precision)
 * @param rounds   number of rounds that were banked
 * @param winner   winner's name, or null if the game ended in a tie
 * @param scores   final scores, highest first
 */
public record GameRecord(LocalDateTime playedAt, int rounds, String winner, List<PlayerScore> scores) {

    public record PlayerScore(String name, int score) { }

    public GameRecord {
        scores = List.copyOf(scores);
    }

    /** Snapshot of a game that has just ended. */
    public static GameRecord from(Flip7Core game) {
        List<PlayerScore> scores = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            scores.add(new PlayerScore(p.getName(), p.getScore()));
        }
        scores.sort(Comparator.comparingInt(PlayerScore::score).reversed());
        String winner = game.getWinner().map(Player::getName).orElse(null);
        return new GameRecord(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                game.getRoundsPlayed(), winner, scores);
    }

    /** Name to show for this game: the winner, or "Tie". */
    public String displayName() {
        return winner == null ? "Tie" : winner;
    }
}
