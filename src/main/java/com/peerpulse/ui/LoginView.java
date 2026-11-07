package com.peerpulse.ui;

import com.peerpulse.model.User;
import com.peerpulse.service.AuthService;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView {
    public static Parent build() {
        Label title = new Label("PeerPulse");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label message = new Label();
        message.setStyle("-fx-text-fill: #c0392b;");
        message.setWrapText(true);

        Button login = new Button("Log in");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        Button register = new Button("Create account");
        register.setMaxWidth(Double.MAX_VALUE);

        login.setOnAction(e -> {
            try {
                User u = new AuthService().login(email.getText(), password.getText());
                Navigator.show(DashboardView.build(u));
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
            }
        });
        register.setOnAction(e -> Navigator.show(RegisterView.build()));

        VBox box = new VBox(10, title, email, password, login, register, message);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(320);
        box.setPadding(new Insets(30));
        return new StackPane(box);
    }
}