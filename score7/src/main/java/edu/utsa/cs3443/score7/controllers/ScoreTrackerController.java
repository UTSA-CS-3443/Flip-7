package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import edu.utsa.cs3443.score7.score7core.Flip7Core;
import edu.utsa.cs3443.score7.score7core.Player;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every player's banked total and this round's pending score.
 * The round only advances when the user presses Next Round.
 */
public class ScoreTrackerController {

    @FXML private Label roundLabel;
    @FXML private VBox playerList;
    @FXML private StackPane overlay;
    @FXML private Label dialogTitle;
    @FXML private Label dialogText;
    @FXML private Button confirmButton;

    private Flip7Core game;
    private Runnable pendingAction;

    @FXML
    private void initialize() {
        game = SceneManager.getGame();
        refresh();
    }

    private void refresh() {
        roundLabel.setText("Round " + game.getRound() + "  ·  first to " + game.getTargetScore());
        playerList.getChildren().clear();
        for (Player p : game.getPlayers()) {
            playerList.getChildren().add(playerRow(p));
        }
    }

    private Button playerRow(Player p) {
        Label name = new Label(p.getName() + ": " + p.getScore());
        name.getStyleClass().add("row-name");

        Label status = new Label(statusText(p));
        status.getStyleClass().add("row-status");
        if (p.getStatus() == Player.Status.BUSTED) {
            status.getStyleClass().add("status-bust");
        } else if (p.hasFlip7()) {
            status.getStyleClass().add("status-flip7");
        } else if (p.getStatus() == Player.Status.STAYED) {
            status.getStyleClass().add("status-frozen");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox content = new HBox(8, name, spacer, status);
        content.setAlignment(Pos.CENTER_LEFT);

        Button row = new Button();
        row.setGraphic(content);
        row.getStyleClass().add("name-box");
        row.setMaxWidth(280);
        row.setPrefWidth(280);
        content.prefWidthProperty().bind(row.widthProperty().subtract(30));
        row.setOnAction(e -> {
            ScoringController scoring = SceneManager.switchTo(SceneManager.SCORING);
            scoring.setPlayer(p.getName());
        });
        return row;
    }

    private static String statusText(Player p) {
        switch (p.getStatus()) {
            case BUSTED:
                return "BUST";
            case STAYED:
                return (p.hasFlip7() ? "FLIP 7 " : "FROZEN ") + "+" + p.getRoundScore();
            default:
                if (p.hasFlip7()) {
                    return "FLIP 7 +" + p.getRoundScore();
                }
                return p.getHand().isEmpty() ? "—" : "+" + p.getRoundScore();
        }
    }

    @FXML
    private void onNextRound() {
        List<String> unscored = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            if (p.getStatus() == Player.Status.ACTIVE && p.getHand().isEmpty()) {
                unscored.add(p.getName());
            }
        }
        if (unscored.isEmpty()) {
            advanceRound();
        } else {
            showDialog("No cards entered",
                    String.join(", ", unscored) + (unscored.size() == 1 ? " has" : " have")
                            + " no cards this round and will score 0. Bank the round anyway?",
                    "Next Round", this::advanceRound);
        }
    }

    @FXML
    private void onEndGame() {
        showDialog("End the game now?",
                "Cards entered this round will not be counted. The highest banked score wins.",
                "End Game", () -> {
                    game.endGame();
                    finishGame();
                });
    }

    @FXML
    private void onConfirm() {
        Runnable action = pendingAction;
        closeDialog();
        if (action != null) {
            action.run();
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void advanceRound() {
        game.nextRound();
        if (game.isGameOver()) {
            finishGame();
        } else {
            refresh();
        }
    }

    private void finishGame() {
        SceneManager.saveFinishedGame();
        SceneManager.switchTo(SceneManager.FINAL_SCORES);
    }

    private void showDialog(String title, String text, String confirmText, Runnable action) {
        dialogTitle.setText(title);
        dialogText.setText(text);
        confirmButton.setText(confirmText);
        pendingAction = action;
        overlay.setVisible(true);
    }

    private void closeDialog() {
        pendingAction = null;
        overlay.setVisible(false);
    }
}
