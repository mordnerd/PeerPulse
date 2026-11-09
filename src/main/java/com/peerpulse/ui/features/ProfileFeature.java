package com.peerpulse.ui.features;

import com.peerpulse.model.*;
import com.peerpulse.service.ProfileService;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ProfileFeature implements FeatureTab {

    @Override
    public String title() {
        return "My Profile";
    }

    @Override
    public boolean appliesTo(Role role) {
        return role == Role.MENTOR || role == Role.MENTEE;
    }

    @Override
    public Parent build(User user) {
        ProfileService service = new ProfileService();
        Profile current = service.get(user);

        TextField year = new TextField(current.academicYear());
        year.setPromptText("e.g. Year 2");
        TextField major = new TextField(current.major());
        major.setPromptText("e.g. Computer Science");
        TextArea skills = new TextArea(current.skills());
        skills.setPromptText("e.g. Java, SQL, Python");
        skills.setPrefRowCount(3);
        TextArea certifications = new TextArea(current.certifications());
        certifications.setPromptText("e.g. First Aid 2024");
        certifications.setPrefRowCount(3);

        Label message = new Label();
        message.setWrapText(true);

        Button save = new Button("Save profile");
        save.setOnAction(e -> {
            try {
                service.save(user, year.getText(), major.getText(),
                        skills.getText(), certifications.getText());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText("Profile saved.");
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });

        VBox form = new VBox(6,
                new Label("Academic year:"), year,
                new Label("Major:"), major,
                new Label("Technical skills (comma separated):"), skills,
                new Label("Certifications:"), certifications,
                save, message);
        form.setPadding(new Insets(20));
        form.setMaxWidth(480);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        return scroll;
    }
}