package com.peerpulse.ui;

import com.peerpulse.model.Role;
import com.peerpulse.service.AuthService;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RegisterView {
    public static Parent build() {
        Label title = new Label("Create your account");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        ChoiceBox<Role> role = new ChoiceBox<>();
        role.getItems().addAll(Role.MENTEE, Role.MENTOR);
        role.setValue(Role.MENTEE);

        TextField name = new TextField();
        name.setPromptText("e.g. Ann Smith");
        TextField email = new TextField();
        email.setPromptText("e.g. ann@gmail.com");
        PasswordField password = new PasswordField();
        password.setPromptText("At least 8 characters");

        TextField academicYear = new TextField();
        academicYear.setPromptText("e.g. Year 2");
        TextField major = new TextField();
        major.setPromptText("e.g. Computer Science");
        TextField skills = new TextField();
        skills.setPromptText("e.g. Java, SQL, Python");
        TextField certifications = new TextField();
        certifications.setPromptText("e.g. First Aid 2024");

        TextArea transcript = new TextArea();
        transcript.setPromptText("e.g. CS101 A, MATH200 B+");
        transcript.setPrefRowCount(3);
        transcript.visibleProperty().bind(role.valueProperty().isEqualTo(Role.MENTOR));
        transcript.managedProperty().bind(transcript.visibleProperty());

        Label message = new Label();
        message.setWrapText(true);

        Button submit = new Button("Register");
        submit.setMaxWidth(Double.MAX_VALUE);
        Button back = new Button("Back to login");
        back.setMaxWidth(Double.MAX_VALUE);

        submit.setOnAction(e -> {
            try {
                new AuthService().register(email.getText(), password.getText(),
                        name.getText(), role.getValue(), transcript.getText(),
                        academicYear.getText(), major.getText(),
                        skills.getText(), certifications.getText());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText(role.getValue() == Role.MENTOR
                        ? "Application submitted. An administrator must approve it before you can log in."
                        : "Account created. You can log in now.");
            } catch (IllegalArgumentException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });
        back.setOnAction(e -> Navigator.show(LoginView.build()));

        VBox box = new VBox(6,
                title,
                new Label("I am a:"), role,
                new Label("Full name:"), name,
                new Label("Email:"), email,
                new Label("Password:"), password,
                new Label("Academic year:"), academicYear,
                new Label("Major:"), major,
                new Label("Technical skills (comma separated):"), skills,
                new Label("Certifications:"), certifications,
                new Label("Previous courses and grades (mentors only):"), transcript,
                submit, back, message);
        box.setMaxWidth(420);
        box.setPadding(new Insets(30));

        // Scrollable, because the form is taller than the window
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }
}