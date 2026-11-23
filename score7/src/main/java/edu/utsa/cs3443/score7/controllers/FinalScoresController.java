package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import edu.utsa.cs3443.score7.score7core.Flip7Core;
import edu.utsa.cs3443.score7.score7core.Player;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shows the finished game's standings. */
public class FinalScoresController {

    @FXML private Label winnerLabel;
    @FXML private Label errorLabel;
    @FXML private VBox scoreList;

    @FXML
    private void initialize() {
        Flip7Core game = SceneManager.getGame();
        Player winner = game.getWinner().orElse(null);
        winnerLabel.setText(winner == null ? "It's a tie!" : winner.getName() + " wins!");

        String saveError = SceneManager.getLastSaveError();
        errorLabel.setText(saveError == null ? "" : saveError);

        List<Player> standings = new ArrayList<>(game.getPlayers());
        standings.sort(Comparator.comparingInt(Player::getScore).reversed());
        for (Player p : standings) {
            Label row = new Label(p.getName() + ": " + p.getScore());
            row.getStyleClass().add("name-box");
            if (p == winner) {
                row.getStyleClass().add("winner-row");
                row.setText("★ " + row.getText());
            }
            scoreList.getChildren().add(row);
        }
    }

    @FXML
    private void onNewGame() {
        List<String> names = new ArrayList<>();
        for (Player p : SceneManager.getGame().getPlayers()) {
            names.add(p.getName());
        }
        SceneManager.setPendingNames(names);
        SceneManager.switchTo(SceneManager.ADD_PLAYERS);
    }

    @FXML
    private void onExit() {
        SceneManager.setGame(null);
        SceneManager.switchTo(SceneManager.MENU);
    }
}
