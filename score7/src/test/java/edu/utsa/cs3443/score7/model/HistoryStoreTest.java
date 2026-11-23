package edu.utsa.cs3443.score7.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryStoreTest {

    @TempDir
    Path dir;

    private static GameRecord game(int day, String winner, String... namesAndScores) {
        List<GameRecord.PlayerScore> scores = new java.util.ArrayList<>();
        for (int i = 0; i < namesAndScores.length; i += 2) {
            scores.add(new GameRecord.PlayerScore(namesAndScores[i], Integer.parseInt(namesAndScores[i + 1])));
        }
        return new GameRecord(LocalDateTime.of(2026, 11, day, 18, 30, 5), 7, winner, scores);
    }

    @Test
    void emptyWhenNoFile() throws IOException {
        assertTrue(new HistoryStore(dir.resolve("none.txt")).load().isEmpty());
    }

    @Test
    void saveLoadDeleteRoundTrip() throws IOException {
        HistoryStore store = new HistoryStore(dir.resolve("sub").resolve("history.txt"));
        GameRecord older = game(1, "A|b;c=d", "A|b;c=d", "205", "Bo b", "150");
        GameRecord newer = game(2, null, "Tom", "210", "Joe", "210");

        store.add(older);
        store.add(newer);

        List<GameRecord> loaded = store.load();
        assertEquals(List.of(newer, older), loaded);
        assertEquals("Tie", loaded.get(0).displayName());

        assertTrue(store.delete(older));
        assertEquals(List.of(newer), store.load());
        assertFalse(store.delete(older));
    }

    @Test
    void corruptLinesAreSkipped() throws IOException {
        Path file = dir.resolve("history.txt");
        HistoryStore store = new HistoryStore(file);
        GameRecord g = game(3, "Tom", "Tom", "200");
        store.add(g);
        Files.writeString(file, "garbage\nnot|a|valid|line=x\n", java.nio.file.StandardOpenOption.APPEND);

        assertEquals(List.of(g), store.load());
    }
}
