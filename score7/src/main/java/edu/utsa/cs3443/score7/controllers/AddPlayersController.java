package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import edu.utsa.cs3443.score7.score7core.Flip7Core;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Enter player names, then start a new game. */
public class AddPlayersController {

    private static final int MAX_NAME_LENGTH = 14;

    @FXML private VBox playerList;
    @FXML private ScrollPane scrollPane;
    @FXML private Label errorLabel;
    @FXML private StackPane overlay;
    @FXML private Label deleteNameLabel;

    private HBox pendingDelete;

    @FXML
    private void initialize() {
        List<String> names = SceneManager.getPendingNames();
        if (names.isEmpty()) {
            addRow("");
        } else {
            for (String name : names) {
                addRow(name);
            }
        }
    }

    @FXML
    private void onAddPlayer() {
        TextField field = addRow("");
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
            field.requestFocus();
        });
    }

    @FXML
    private void onBack() {
        SceneManager.switchTo(SceneManager.MENU);
    }

    @FXML
    private void onDone() {
        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Node row : playerList.getChildren()) {
            String name = fieldOf((HBox) row).getText().trim();
            if (name.isEmpty()) {
                continue;
            }
            if (!seen.add(name.toLowerCase())) {
                errorLabel.setText("Each player needs a different name (\"" + name + "\" is used twice).");
                return;
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            errorLabel.setText("Add at least one player.");
            return;
        }

        Flip7Core game = new Flip7Core();
        for (String name : names) {
            game.addPlayer(name);
        }
        SceneManager.setGame(game);
        SceneManager.setPendingNames(names);
        SceneManager.switchTo(SceneManager.SCORE_TRACKER);
    }

    @FXML
    private void onConfirmDelete() {
        if (pendingDelete != null) {
            playerList.getChildren().remove(pendingDelete);
            if (playerList.getChildren().isEmpty()) {
                addRow("");
            }
        }
        closeOverlay();
    }

    @FXML
    private void onCancelDelete() {
        closeOverlay();
    }

    private TextField addRow(String name) {
        TextField field = new TextField(name);
        field.setPromptText("Player name");
        field.getStyleClass().add("name-box");
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_NAME_LENGTH ? change : null));
        field.textProperty().addListener((obs, old, now) -> errorLabel.setText(""));

        Button delete = new Button("✕");
        delete.getStyleClass().add("icon-button");

        HBox row = new HBox(6, field, delete);
        row.setAlignment(Pos.CENTER);
        delete.setOnAction(e -> askDelete(row));
        playerList.getChildren().add(row);
        return field;
    }

    private void askDelete(HBox row) {
        String name = fieldOf(row).getText().trim();
        if (name.isEmpty()) {
            // nothing typed yet: no need to confirm
            pendingDelete = row;
            onConfirmDelete();
            return;
        }
        pendingDelete = row;
        deleteNameLabel.setText(name);
        overlay.setVisible(true);
    }

    private void closeOverlay() {
        pendingDelete = null;
        overlay.setVisible(false);
    }

    private static TextField fieldOf(HBox row) {
        return (TextField) row.getChildren().get(0);
    }
}
