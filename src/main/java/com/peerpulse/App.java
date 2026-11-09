package com.peerpulse;

import com.peerpulse.db.Database;
import com.peerpulse.service.AuthService;
import com.peerpulse.service.CourseService;
import com.peerpulse.ui.LoginView;
import com.peerpulse.ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override

    public void start(Stage stage) {

        Database.init();
        new AuthService().seedAdmin();
        new CourseService().seedDefaults();
        Navigator.init(stage);
        stage.setTitle("PeerPulse");
        Navigator.show(LoginView.build());
        stage.show();

    }

    public static void main(String[] args) { launch(args); }
}