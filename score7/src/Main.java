import score7core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {

    // Relative path = the working directory, which is the project root when run from an IDE
    private static final Path RECORDS_FILE = Paths.get("game_records.txt");

    public static void main(String[] args) {
        System.out.println(
                "                                   ______ \n" +
                        "                            ______|____  |\n" +
                        "                     _____ |  ____|   / / \n" +
                        "                ____|  __ \\| |__     / /  \n" +
                        "         _____ / __ \\ |__) |  __|   / /   \n" +
                        "  _____ / ____| |  ||  _  /| |____ |_/    \n" +
                        " / ____| |    | |  || | \\ \\|______|       \n" +
                        "| (___ | |    | |__||_|  \\_|              \n" +
                        " \\___ \\| |____ \\____/                     \n" +
                        " ____) |\\_____|                           \n" +
                        "|_____/                                   "
        );

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println();
            System.out.println("=== Main Menu ===");
            System.out.println("(N)ew game\t(V)iew game records\t(Q)uit");

            if (!sc.hasNextLine()) {
                break; // input closed
            }
            String choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("n")) {
                playGame(sc);
            } else if (choice.equalsIgnoreCase("v")) {
                viewRecords(sc);
            } else if (choice.equalsIgnoreCase("q")) {
                break;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Game
    // ---------------------------------------------------------------

    static void playGame(Scanner sc) {
        Flip7Core game = new Flip7Core();

        System.out.println("Add players (press Enter on an empty line when done)");

        while (sc.hasNextLine()) {
            String name = sc.nextLine();

            if (name.isEmpty()) {
                break;
            }

            try {
                game.addPlayer(name);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }

        if (game.getPlayers().isEmpty()) {
            System.out.println("No players added. Returning to menu.");
            return;
        }

        while (!game.isGameOver()) {
            System.out.println("Round:\t" + game.getRound());
            for(Player p : game.getPlayers()){
                System.out.print(p.getName() + ": " + p.getScore() + "\t");
            }
            System.out.println("\nWhose turn is it?");

            if (!sc.hasNextLine()) {
                return; // input closed mid-game
            }
            String turn = sc.nextLine();

            Player player;
            try {
                player = game.getPlayer(turn);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                continue;
            }

            System.out.println(player.getName());
            System.out.println("Total:\t" + player.getScore() + "\tRound:\t" + player.getRoundScore() + "\tStatus:\t" + player.getStatus());
            System.out.println("Hand:\t" + player.getHand());
            System.out.println("Actions?");
            System.out.println("(I)nput Cards\t(S)tay\t(B)ust");

            if (!sc.hasNextLine()) {
                return;
            }

            String action = sc.nextLine();

            try {
                if (action.equalsIgnoreCase("b")) {
                    game.bust(turn);
                } else if (action.equalsIgnoreCase("s")) {
                    game.stay(turn);
                } else if (action.equalsIgnoreCase("i")) {
                    inputCards(sc, game, turn);
                }
            } catch (IllegalStateException e) {
                // e.g. the player is already out of this round
                System.out.println(e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Game over!");
        game.getWinner().ifPresent(w -> System.out.println("Winner: " + w.getName() + " with " + w.getScore()));
        saveRecord(game);
    }

    /** Reads cards until a blank line, or until the card ends the player's turn. */
    static void inputCards(Scanner sc, Flip7Core game, String turn) {
        while (sc.hasNextLine()) {
            String card = sc.nextLine();

            if (card.isEmpty()) {
                break;
            }

            try {
                CardResult result = game.addCard(turn, parseCard(card));
                System.out.println(result);

                // Only ADDED and SECOND_CHANCE_USED leave the player still in the round
                if (result != CardResult.ADDED && result != CardResult.SECOND_CHANCE_USED) {
                    break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Records
    // ---------------------------------------------------------------

    /**
     * Appends one line to the records file:
     * timestamp|winner|rounds|name=score;name=score;...
     */
    static void saveRecord(Flip7Core game) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String winner = game.getWinner().map(p -> sanitize(p.getName())).orElse("-");

        StringBuilder scores = new StringBuilder();
        for (Player p : game.getPlayers()) {
            if (scores.length() > 0) {
                scores.append(";");
            }
            scores.append(sanitize(p.getName())).append("=").append(p.getScore());
        }

        String line = timestamp + "|" + winner + "|" + game.getRound() + "|" + scores;

        try {
            Files.write(RECORDS_FILE, Collections.singletonList(line),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Game record saved to " + RECORDS_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not save game record: " + e.getMessage());
        }
    }

    /** Keeps player names from breaking the file format. */
    static String sanitize(String name) {
        return name.replace('|', '_').replace(';', '_').replace('=', '_');
    }

    static List<String> loadRecords() {
        List<String> lines = new ArrayList<>();
        if (!Files.exists(RECORDS_FILE)) {
            return lines;
        }
        try {
            for (String line : Files.readAllLines(RECORDS_FILE)) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read game records: " + e.getMessage());
        }
        return lines;
    }

    static void writeRecords(List<String> records) {
        try {
            Files.write(RECORDS_FILE, records); // overwrites the file
        } catch (IOException e) {
            System.out.println("Could not update game records: " + e.getMessage());
        }
    }

    static void viewRecords(Scanner sc) {
        while (true) {
            List<String> records = loadRecords();

            if (records.isEmpty()) {
                System.out.println("No game records found.");
                return;
            }

            System.out.println();
            System.out.println("=== Game Records ===");
            for (int i = 0; i < records.size(); i++) {
                printRecord(i + 1, records.get(i));
            }

            System.out.println();
            System.out.println("(D)elete a record\tdelete (A)ll records\t(B)ack");
            if (!sc.hasNextLine()) {
                return;
            }
            String choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("b") || choice.isEmpty()) {
                return;
            } else if (choice.equalsIgnoreCase("d")) {
                System.out.println("Record number to delete:");
                String input = sc.nextLine().trim();
                try {
                    int index = Integer.parseInt(input);
                    if (index < 1 || index > records.size()) {
                        System.out.println("No record with that number.");
                    } else {
                        records.remove(index - 1);
                        writeRecords(records);
                        System.out.println("Record " + index + " deleted.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a number.");
                }
            } else if (choice.equalsIgnoreCase("a")) {
                System.out.println("Delete ALL records? This cannot be undone. (y/N)");
                if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                    writeRecords(new ArrayList<>());
                    System.out.println("All records deleted.");
                }
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    static void printRecord(int number, String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            System.out.println("#" + number + "  (unreadable record)");
            return;
        }
        System.out.println("#" + number + "  " + parts[0] + "   Winner: " + parts[1] + "   Rounds: " + parts[2]);
        for (String entry : parts[3].split(";")) {
            String[] ns = entry.split("=", 2);
            if (ns.length == 2) {
                System.out.println("     " + ns[0] + ":\t" + ns[1]);
            }
        }
    }

    // ---------------------------------------------------------------
    // Card parsing
    // ---------------------------------------------------------------

    static Cards parseCard(String input) {
        String s = input.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        switch (s) {
            case "X2": case "*2":                   return Cards.XTWO;
            case "+2":                              return Cards.PTWO;
            case "+4":                              return Cards.PFOUR;
            case "+6":                              return Cards.PSIX;
            case "+8":                              return Cards.PEIGHT;
            case "+10":                             return Cards.PTEN;
            case "F3": case "FLIP3": case "FLIP_3": return Cards.FLIP_THREE;
            case "SC":                              return Cards.SECOND_CHANCE;
        }

        if (s.matches("\\d{1,2}")) {
            int n = Integer.parseInt(s);
            for (Cards c : Cards.values()) {
                if (c.isNumber() && c.getValue() == n) {
                    return c;
                }
            }
        }

        try {
            return Cards.valueOf(s); // enum names: "seven", "freeze", "second chance", "flip_three"...
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown card: " + input);
        }
    }
}