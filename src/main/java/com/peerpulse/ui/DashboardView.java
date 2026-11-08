package com.peerpulse.ui;

import com.peerpulse.model.*;
import com.peerpulse.ui.features.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DashboardView {

    public static Parent build(User user) {
        Label welcome = new Label("Welcome, " + user.fullName() + " (" + user.role() + ")");
        welcome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button logout = new Button("Log out");
        logout.setOnAction(e -> Navigator.show(LoginView.build()));

        HBox header = new HBox(20, welcome, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        if (user.role() == Role.ADMIN) {
            tabs.getTabs().add(new Tab("Applications", AdminDashboardView.build(user)));
        }
        for (FeatureTab feature : FeatureRegistry.all()) {
            if (feature.appliesTo(user.role())) {
                tabs.getTabs().add(new Tab(feature.title(), feature.build(user)));
            }
        }

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabs);
        return root;
    }
}