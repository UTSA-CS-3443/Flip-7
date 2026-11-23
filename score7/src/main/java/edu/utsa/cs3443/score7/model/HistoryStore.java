package edu.utsa.cs3443.score7.model;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Saves finished games to a text file on the user's device.
 * Default location: {@code ~/.score7/history.txt}. One game per line:
 * {@code isoDateTime|rounds|winner|name=score;name=score}, with names URL-encoded
 * so separators inside names are safe. Unreadable lines are skipped.
 */
public class HistoryStore {

    private final Path file;

    public HistoryStore() {
        this(Paths.get(System.getProperty("user.home"), ".score7", "history.txt"));
    }

    public HistoryStore(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    /** All saved games, newest first. */
    public List<GameRecord> load() throws IOException {
        List<GameRecord> records = new ArrayList<>();
        if (!Files.exists(file)) {
            return records;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            GameRecord r = parse(line);
            if (r != null) {
                records.add(r);
            }
        }
        records.sort(Comparator.comparing(GameRecord::playedAt).reversed());
        return records;
    }

    public void add(GameRecord record) throws IOException {
        ensureDirectory();
        Files.writeString(file, format(record) + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Removes one saved game. Returns false if it was not found. */
    public boolean delete(GameRecord record) throws IOException {
        List<GameRecord> records = load();
        if (!records.remove(record)) {
            return false;
        }
        ensureDirectory();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        for (GameRecord r : records) {
            lines.add(format(r));
        }
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // ---- line format ----

    static String format(GameRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.playedAt()).append('|')
          .append(r.rounds()).append('|')
          .append(r.winner() == null ? "" : encode(r.winner())).append('|');
        for (int i = 0; i < r.scores().size(); i++) {
            GameRecord.PlayerScore s = r.scores().get(i);
            if (i > 0) {
                sb.append(';');
            }
            sb.append(encode(s.name())).append('=').append(s.score());
        }
        return sb.toString();
    }

    static GameRecord parse(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 4) {
                return null;
            }
            LocalDateTime playedAt = LocalDateTime.parse(parts[0]);
            int rounds = Integer.parseInt(parts[1]);
            String winner = parts[2].isEmpty() ? null : decode(parts[2]);
            List<GameRecord.PlayerScore> scores = new ArrayList<>();
            if (!parts[3].isEmpty()) {
                for (String entry : parts[3].split(";")) {
                    String[] kv = entry.split("=", 2);
                    scores.add(new GameRecord.PlayerScore(decode(kv[0]), Integer.parseInt(kv[1])));
                }
            }
            return new GameRecord(playedAt, rounds, winner, scores);
        } catch (RuntimeException e) {
            return null; // corrupt line
        }
    }

    private void ensureDirectory() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
