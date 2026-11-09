package com.peerpulse.ui;

import com.peerpulse.model.*;
import com.peerpulse.service.AdminService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.function.Function;

public class AdminDashboardView {

    public static Parent build(User admin) {
        AdminService service = new AdminService();

        Label title = new Label("Admin dashboard");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Summary cards (wireframe 13)
        Label pendingCount = bigNumber();
        Label activeCount = bigNumber();
        Label reportCount = bigNumber();
        HBox cards = new HBox(15,
                card("Pending mentors", pendingCount),
                card("Active users", activeCount),
                card("Open reports", reportCount));

        // Application review (wireframe 14)
        Label tableTitle = new Label("Pending mentor applications");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TableView<MentorApplication> table = new TableView<>();
        table.setPlaceholder(new Label("No pending applications."));
        table.setPrefHeight(300);
        table.getColumns().addAll(
                column("Name", 130, MentorApplication::fullName),
                column("Email", 170, MentorApplication::email),
                column("Academic year", 110, MentorApplication::academicYear),
                column("Major", 150, MentorApplication::major),
                column("Skills", 180, MentorApplication::skills),
                column("Certifications", 180, MentorApplication::certifications),
                column("Transcript", 300, MentorApplication::transcript));

        Label message = new Label();
        message.setWrapText(true);

        Runnable reload = () -> {
            table.setItems(FXCollections.observableArrayList(service.pendingMentors(admin)));
            AdminCounts counts = service.counts(admin);
            pendingCount.setText(String.valueOf(counts.pendingMentors()));
            activeCount.setText(String.valueOf(counts.activeUsers()));
            reportCount.setText(String.valueOf(counts.openReports()));
        };
        reload.run();

        Button approve = new Button("Approve");
        Button reject = new Button("Reject");
        Button refresh = new Button("Refresh");
        Button logout = new Button("Log out");

        approve.setOnAction(e -> {
            MentorApplication selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { message.setText("Select an application first."); return; }
            try {
                service.approve(admin, selected.id());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText(selected.fullName() + " approved. They can now log in.");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });

        reject.setOnAction(e -> {
            MentorApplication selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { message.setText("Select an application first."); return; }
            try {
                service.reject(admin, selected.id());
                message.setStyle("-fx-text-fill: #1e8449;");
                message.setText(selected.fullName() + " rejected.");
                reload.run();
            } catch (RuntimeException ex) {
                message.setStyle("-fx-text-fill: #c0392b;");
                message.setText(ex.getMessage());
            }
        });

        refresh.setOnAction(e -> reload.run());
        logout.setOnAction(e -> Navigator.show(LoginView.build()));

        HBox buttons = new HBox(10, approve, reject, refresh, logout);

        VBox root = new VBox(14, title, cards, tableTitle, table, buttons, message);
        root.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private static Label bigNumber() {
        Label l = new Label("0");
        l.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        return l;
    }

    private static VBox card(String caption, Label value) {
        VBox box = new VBox(4, value, new Label(caption));
        box.setPadding(new Insets(12));
        box.setMinWidth(160);
        box.setStyle("-fx-background-color: #f2f4f7; -fx-background-radius: 6;");
        return box;
    }

    private static TableColumn<MentorApplication, String> column(
            String title, double width, Function<MentorApplication, String> getter) {
        TableColumn<MentorApplication, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(d -> new SimpleStringProperty(nz(getter.apply(d.getValue()))));
        return col;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}