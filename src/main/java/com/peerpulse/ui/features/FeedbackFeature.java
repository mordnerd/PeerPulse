package com.peerpulse.ui.features;

import com.peerpulse.model.FeedbackBooking;
import com.peerpulse.model.Role;
import com.peerpulse.model.SessionFeedback;
import com.peerpulse.model.User;
import com.peerpulse.service.FeedbackService;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class FeedbackFeature implements FeatureTab {
    private final FeedbackService service = new FeedbackService();

    @Override
    public String title() {
        return "Feedback";
    }

    @Override
    public boolean appliesTo(Role role) {
        return role == Role.MENTOR || role == Role.MENTEE;
    }

    @Override
    public Parent build(User user) {
        VBox content;
        try {
            if (user.role() == Role.MENTOR) {
                content = buildMentorView(user);
            } else {
                content = buildMenteeView(user);
            }
        } catch (IllegalStateException e) {
            content = new VBox(new Label(e.getMessage()));
        }
        content.setPadding(new Insets(20));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private VBox buildMentorView(User mentor) {
        ComboBox<FeedbackBooking> bookings = new ComboBox<>();
        bookings.getItems().setAll(service.bookingsForMentor(mentor));
        bookings.setPromptText("Select a completed booking");

        TextArea comment = new TextArea();
        comment.setPromptText("Write feedback for the mentee");
        comment.setWrapText(true);
        comment.setPrefRowCount(4);

        Label message = new Label();
        message.setWrapText(true);
        if (bookings.getItems().isEmpty()) {
            message.setText("No completed bookings are waiting for feedback.");
        }

        Button save = new Button("Save feedback");
        save.disableProperty().bind(bookings.valueProperty().isNull());
        save.setOnAction(event -> {
            FeedbackBooking booking = bookings.getValue();
            try {
                service.submit(mentor, booking.id(), comment.getText());
                bookings.setValue(null);
                bookings.getItems().remove(booking);
                comment.clear();
                message.setText("Feedback saved.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                message.setText(e.getMessage());
            }
        });

        return new VBox(10, new Label("Completed bookings"), bookings,
                new Label("Feedback for the mentee"), comment, save, message);
    }

    private VBox buildMenteeView(User mentee) {
        VBox content = new VBox(10, new Label("My Feedback"));
        List<SessionFeedback> entries = service.forMentee(mentee);
        if (entries.isEmpty()) {
            content.getChildren().add(new Label("You have no session feedback yet."));
        }
        for (SessionFeedback entry : entries) {
            Label feedback = new Label(entry.courseTitle() + " · " + entry.startTime()
                    + "\nFrom " + entry.mentorName() + "\n" + entry.comment());
            feedback.setWrapText(true);
            content.getChildren().add(feedback);
        }
        return content;
    }
}
