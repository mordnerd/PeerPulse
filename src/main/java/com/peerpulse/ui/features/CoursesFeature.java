package com.peerpulse.ui.features;

import com.peerpulse.model.Course;
import com.peerpulse.model.Role;
import com.peerpulse.model.User;
import com.peerpulse.service.CourseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CoursesFeature implements FeatureTab {
    private final CourseService service = new CourseService();

    @Override
    public String title() {
        return "Courses";
    }

    @Override
    public boolean appliesTo(Role role) {
        return role == Role.MENTOR || role == Role.MENTEE || role == Role.ADMIN;
    }

    @Override
    public Parent build(User user) {
        if (user.role() == Role.ADMIN) {
            return buildAdminView(user);
        }
        if (user.role() == Role.MENTOR) {
            return buildMentorView(user);
        }
        return buildMenteeView(user);
    }

    private Parent buildMenteeView(User user) {
        Label heading = new Label("Browse courses");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label caption = new Label("Courses available for academic support.");
        caption.setWrapText(true);

        TableView<Course> table = courseTable();
        table.setItems(FXCollections.observableArrayList(service.listAll(user)));
        table.setPlaceholder(new Label("No courses have been published yet."));

        VBox content = new VBox(10, heading, caption, table);
        content.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Parent buildMentorView(User user) {
        Label heading = new Label("Mentoring subjects");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label caption = new Label("Select the courses you can mentor.");
        caption.setWrapText(true);

        TableView<Course> table = courseTable();
        TableColumn<Course, String> status = new TableColumn<>("Status");
        status.setPrefWidth(120);
        status.setCellValueFactory(d -> new SimpleStringProperty(
                service.isSignedUp(user, d.getValue().id()) ? "Signed up" : "Not signed up"));
        table.getColumns().add(status);

        Label message = new Label();
        message.setWrapText(true);

        Runnable reload = () -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            table.setItems(FXCollections.observableArrayList(service.listAll(user)));
            table.refresh();
            if (selected != null) {
                table.getItems().stream()
                        .filter(course -> course.id() == selected.id())
                        .findFirst()
                        .ifPresent(course -> table.getSelectionModel().select(course));
            }
        };
        reload.run();

        Button signUp = new Button("Sign up");
        Button quit = new Button("Quit");
        signUp.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Select a course first.");
                return;
            }
            try {
                service.signUp(user, selected.id());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText("You are now mentoring " + selected.code() + ".");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });
        quit.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Select a course first.");
                return;
            }
            try {
                service.quit(user, selected.id());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText("You withdrew from " + selected.code() + ".");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });

        VBox content = new VBox(10, heading, caption, table, new HBox(10, signUp, quit), message);
        content.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Parent buildAdminView(User admin) {
        Label heading = new Label("Manage course repository");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField code = new TextField();
        code.setPromptText("Code");
        TextField title = new TextField();
        title.setPromptText("Title");
        TextField description = new TextField();
        description.setPromptText("Description");
        HBox form = new HBox(8, code, title, description);
        HBox.setHgrow(title, Priority.ALWAYS);
        HBox.setHgrow(description, Priority.ALWAYS);

        TableView<Course> table = courseTable();
        Label message = new Label();
        message.setWrapText(true);

        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(service.listAll(admin)));
        reload.run();

        Button add = new Button("Add course");
        Button delete = new Button("Delete");
        add.setOnAction(e -> {
            try {
                service.add(admin, code.getText(), title.getText(), description.getText());
                code.clear();
                title.clear();
                description.clear();
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText("Course added.");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });
        delete.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Select a course first.");
                return;
            }
            try {
                service.delete(admin, selected.id());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText(selected.code() + " deleted.");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });

        VBox content = new VBox(10, heading, form, new HBox(10, add, delete), table, message);
        content.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private static TableView<Course> courseTable() {
        TableView<Course> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No courses in the repository."));
        TableColumn<Course, String> code = new TableColumn<>("Course code");
        code.setPrefWidth(120);
        code.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().code()));
        TableColumn<Course, String> title = new TableColumn<>("Title / description");
        title.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().title()
                        + (d.getValue().description().isBlank() ? "" : " — " + d.getValue().description())));
        table.getColumns().addAll(code, title);
        return table;
    }
}
