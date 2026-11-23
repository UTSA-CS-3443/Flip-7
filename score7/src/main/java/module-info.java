module edu.utsa.cs3443.score7 {
    requires javafx.controls;
    requires javafx.fxml;

    opens edu.utsa.cs3443.score7.controllers to javafx.fxml;

    exports edu.utsa.cs3443.score7;
    exports edu.utsa.cs3443.score7.controllers;
    exports edu.utsa.cs3443.score7.model;
    exports edu.utsa.cs3443.score7.score7core;
}
