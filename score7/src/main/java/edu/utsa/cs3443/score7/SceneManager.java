package edu.utsa.cs3443.score7;

import edu.utsa.cs3443.score7.model.GameRecord;
import edu.utsa.cs3443.score7.model.HistoryStore;
import edu.utsa.cs3443.score7.score7core.Flip7Core;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The JavaFX application. Owns the window, switches between FXML screens, and holds
 * the state shared between screens (the current game and the history store).
 */
public class SceneManager extends Application {

    public static final String MENU = "menu-view.fxml";
    public static final String ADD_PLAYERS = "add-players-view.fxml";
    public static final String SCORE_TRACKER = "score-tracker-view.fxml";
    public static final String SCORING = "scoring-view.fxml";
    public static final String FINAL_SCORES = "final-scores-view.fxml";
    public static final String HISTORY = "history-view.fxml";

    /** Phone-sized window. */
//    private static final double WIDTH = 360;
//    private static final double HEIGHT = 720;

    /** Desktop-sized window. */
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;

    private static Stage stage;
    private static Flip7Core game;
    private static List<String> pendingNames = new ArrayList<>();
    private static final HistoryStore history = new HistoryStore();
    private static String lastSaveError;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Flip 7 Score Tracker");
        stage.setResizable(false);
        switchTo(MENU);
        stage.show();
    }

    /**
     * Loads an FXML screen into the window and returns its controller so the caller
     * can hand it data.
     */
    public static <T> T switchTo(String fxml) {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + fxml, e);
        }
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(SceneManager.class.getResource("style.css").toExternalForm());
        stage.setScene(scene);
        return loader.getController();
    }

    // ---- shared state ----

    public static Flip7Core getGame() {
        return game;
    }

    public static void setGame(Flip7Core newGame) {
        game = newGame;
    }

    /** Names to prefill on the Add Players screen (used by "Start new game"). */
    public static List<String> getPendingNames() {
        return pendingNames;
    }

    public static void setPendingNames(List<String> names) {
        pendingNames = new ArrayList<>(names);
    }

    public static HistoryStore getHistory() {
        return history;
    }

    /** Saves the finished current game to history. Returns false (and records why) on failure. */
    public static boolean saveFinishedGame() {
        lastSaveError = null;
        try {
            history.add(GameRecord.from(game));
            return true;
        } catch (IOException e) {
            lastSaveError = "Could not save game to history: " + e.getMessage();
            return false;
        }
    }

    public static String getLastSaveError() {
        return lastSaveError;
    }
}
