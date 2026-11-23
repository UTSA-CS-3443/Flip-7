package edu.utsa.cs3443.score7.controllers;

import edu.utsa.cs3443.score7.SceneManager;
import javafx.fxml.FXML;

import java.util.List;

/** Main menu: Play or History. */
public class MenuController {

    @FXML
    private void onPlay() {
        SceneManager.setPendingNames(List.of());
        SceneManager.switchTo(SceneManager.ADD_PLAYERS);
    }

    @FXML
    private void onHistory() {
        SceneManager.switchTo(SceneManager.HISTORY);
    }
}
