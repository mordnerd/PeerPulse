package com.peerpulse.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Navigator {
    private static Stage stage;

    private Navigator() {}

    public static void init(Stage s) { stage = s; }

    public static void show(Parent root) {
        if (stage.getScene() == null) stage.setScene(new Scene(root, 900, 600));
        else stage.getScene().setRoot(root);
    }
}