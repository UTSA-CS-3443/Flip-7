package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import edu.utsa.cs3443.score7.model.GameRecord;
import edu.utsa.cs3443.score7.model.HistoryStore;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Lists saved games; tapping one shows its scores and offers to delete it. */
public class HistoryController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private VBox gameList;
    @FXML private Label errorLabel;
    @FXML private StackPane overlay;
    @FXML private Label detailTitle;
    @FXML private Label detailText;

    private final HistoryStore store = SceneManager.getHistory();
    private GameRecord selected;

    @FXML
    private void initialize() {
        refresh();
    }

    private void refresh() {
        gameList.getChildren().clear();
        List<GameRecord> games;
        try {
            games = store.load();
        } catch (IOException e) {
            errorLabel.setText("Could not read history: " + e.getMessage());
            return;
        }
        if (games.isEmpty()) {
            Label empty = new Label("No games yet");
            empty.getStyleClass().add("subtitle");
            gameList.getChildren().add(empty);
            return;
        }
        for (GameRecord g : games) {
            gameList.getChildren().add(gameRow(g));
        }
    }

    private Button gameRow(GameRecord g) {
        Label name = new Label(g.displayName() + ":");
        name.getStyleClass().add("row-name");

        Label date = new Label(DATE.format(g.playedAt()));
        Label time = new Label(TIME.format(g.playedAt()));
        date.getStyleClass().add("row-date");
        time.getStyleClass().add("row-date");
        VBox when = new VBox(date, time);
        when.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox content = new HBox(8, name, spacer, when);
        content.setAlignment(Pos.CENTER_LEFT);

        Button row = new Button();
        row.setGraphic(content);
        row.getStyleClass().add("name-box");
        row.setPrefWidth(260);
        row.setMaxWidth(260);
        content.prefWidthProperty().bind(row.widthProperty().subtract(30));
        row.setOnAction(e -> showDetails(g));
        return row;
    }

    private void showDetails(GameRecord g) {
        selected = g;
        detailTitle.setText(g.winner() == null ? "Tie game" : g.winner() + " won");
        StringBuilder sb = new StringBuilder();
        sb.append(DATE.format(g.playedAt())).append("  ").append(TIME.format(g.playedAt()))
          .append("  ·  ").append(g.rounds()).append(g.rounds() == 1 ? " round" : " rounds");
        for (GameRecord.PlayerScore s : g.scores()) {
            sb.append('\n').append(s.name()).append(": ").append(s.score());
        }
        detailText.setText(sb.toString());
        overlay.setVisible(true);
    }

    @FXML
    private void onConfirmDelete() {
        if (selected != null) {
            try {
                store.delete(selected);
            } catch (IOException e) {
                errorLabel.setText("Could not delete game: " + e.getMessage());
            }
        }
        closeOverlay();
        refresh();
    }

    @FXML
    private void onCancelDelete() {
        closeOverlay();
    }

    @FXML
    private void onBack() {
        SceneManager.switchTo(SceneManager.MENU);
    }

    private void closeOverlay() {
        selected = null;
        overlay.setVisible(false);
    }
}
