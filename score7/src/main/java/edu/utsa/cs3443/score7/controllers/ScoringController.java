package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import edu.utsa.cs3443.score7.score7core.Cards;
import edu.utsa.cs3443.score7.score7core.Flip7Core;
import edu.utsa.cs3443.score7.score7core.Player;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

import java.util.EnumMap;
import java.util.Map;

/**
 * Card picker for one player. Each toggle adds or removes a card in the core
 * immediately; Bust / Freeze lock the grid until they are deselected. Nothing is
 * banked until Next Round on the tracker.
 */
public class ScoringController {

    /** Cards shown in the grid, in display order. */
    private static final Cards[] GRID = {
            Cards.ZERO, Cards.ONE, Cards.TWO, Cards.THREE,
            Cards.FOUR, Cards.FIVE, Cards.SIX, Cards.SEVEN,
            Cards.EIGHT, Cards.NINE, Cards.TEN, Cards.ELEVEN,
            Cards.TWELVE, Cards.XTWO, Cards.PTWO, Cards.PFOUR,
            Cards.PSIX, Cards.PEIGHT, Cards.PTEN
    };

    /** Face colours for number cards 0-12, loosely following the real deck. */
    private static final String[] NUMBER_COLORS = {
            "#7a7a7a", "#8a6d3b", "#b8a000", "#d6336c", "#1c7ed6", "#2b8a3e", "#ae3ec9",
            "#e8590c", "#5c940d", "#c2255c", "#d9480f", "#1971c2", "#495057"
    };

    private static final int COLUMNS = 4;

    @FXML private Label nameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label flip7Label;
    @FXML private Label errorLabel;
    @FXML private GridPane cardGrid;
    @FXML private ToggleButton bustToggle;
    @FXML private ToggleButton freezeToggle;

    private final Map<Cards, ToggleButton> buttons = new EnumMap<>(Cards.class);
    private Flip7Core game;
    private String playerName;

    @FXML
    private void initialize() {
        game = SceneManager.getGame();
        for (int i = 0; i < GRID.length; i++) {
            Cards card = GRID[i];
            ToggleButton b = new ToggleButton(cardLabel(card));
            b.getStyleClass().add("card");
            switch (card.getKind()) {
                case NUMBER:
                    b.setStyle("-fx-text-fill: " + NUMBER_COLORS[card.getValue()] + ";");
                    break;
                case MULTIPLIER:
                    b.getStyleClass().add("card-multiplier");
                    break;
                default:
                    b.getStyleClass().add("card-modifier");
                    break;
            }
            b.setOnAction(e -> onCardToggled(card, b));
            buttons.put(card, b);
            cardGrid.add(b, i % COLUMNS, i / COLUMNS);
        }
    }

    /** Called by the tracker right after this screen is loaded. */
    public void setPlayer(String name) {
        playerName = name;
        nameLabel.setText(name);
        refresh();
    }

    private void onCardToggled(Cards card, ToggleButton button) {
        errorLabel.setText("");
        try {
            if (button.isSelected()) {
                game.addCard(playerName, card);
            } else {
                game.removeCard(playerName, card);
            }
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
        }
        refresh();
    }

    @FXML
    private void onBust() {
        setStatus(bustToggle.isSelected() ? Player.Status.BUSTED : Player.Status.ACTIVE);
    }

    @FXML
    private void onFreeze() {
        setStatus(freezeToggle.isSelected() ? Player.Status.STAYED : Player.Status.ACTIVE);
    }

    private void setStatus(Player.Status status) {
        errorLabel.setText("");
        Player p = game.getPlayer(playerName);
        if (p.getStatus() != Player.Status.ACTIVE) {
            game.reopen(playerName);
        }
        if (status == Player.Status.BUSTED) {
            game.bust(playerName);
        } else if (status == Player.Status.STAYED) {
            game.stay(playerName);
        }
        refresh();
    }

    @FXML
    private void onDone() {
        SceneManager.switchTo(SceneManager.SCORE_TRACKER);
    }

    /** Syncs every control with the core's view of this player. */
    private void refresh() {
        Player p = game.getPlayer(playerName);
        boolean active = p.getStatus() == Player.Status.ACTIVE;
        boolean flip7 = p.hasFlip7();

        for (Map.Entry<Cards, ToggleButton> e : buttons.entrySet()) {
            Cards card = e.getKey();
            ToggleButton b = e.getValue();
            boolean held = p.getHand().contains(card);
            b.setSelected(held);
            b.setDisable(!active || (flip7 && card.isNumber() && !held));
        }

        bustToggle.setSelected(p.getStatus() == Player.Status.BUSTED);
        freezeToggle.setSelected(p.getStatus() == Player.Status.STAYED);
        flip7Label.setVisible(flip7);
        flip7Label.setManaged(flip7);
        flip7Label.setText("FLIP 7!  +" + game.getFlip7Bonus() + " bonus");

        int round = p.getRoundScore();
        String state = p.getStatus() == Player.Status.BUSTED ? "BUST — "
                : p.getStatus() == Player.Status.STAYED ? "Frozen — " : "";
        scoreLabel.setText(state + "This round: " + round + "   ·   Total: " + (p.getScore() + round));
    }

    private static String cardLabel(Cards card) {
        switch (card.getKind()) {
            case NUMBER:     return String.valueOf(card.getValue());
            case MULTIPLIER: return "x" + card.getValue();
            default:         return "+" + card.getValue();
        }
    }
}
