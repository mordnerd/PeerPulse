package com.peerpulse.ui.features;

import com.peerpulse.model.*;
import com.peerpulse.service.SchedulingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Mentors publish slots; mentees reserve and cancel them (wireframes 4 and 10). */
public final class SchedulingFeature implements FeatureTab {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private final SchedulingService service = new SchedulingService();

    @Override public String title() { return "Sessions"; }
    @Override public boolean appliesTo(Role role) {
        return role == Role.MENTOR || role == Role.MENTEE;
    }

    @Override public Parent build(User user) {
        VBox content = user.role() == Role.MENTOR ? mentorView(user) : menteeView(user);
        content.setPadding(new Insets(18));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private VBox mentorView(User mentor) {
        Label title = header("Manage availability");
        Label instruction = new Label("Publish a future time slot. Only available (unbooked) slots may be edited or removed.");
        instruction.setWrapText(true);

        DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<LocalTime> start = startTimes();
        ComboBox<Integer> minutes = durations();
        HBox form = new HBox(12, new VBox(4, new Label("Date"), date),
                new VBox(4, new Label("Start time"), start),
                new VBox(4, new Label("Duration (minutes)"), minutes));

        TableView<SessionSlot> table = new TableView<>();
        table.setPlaceholder(new Label("No sessions yet. Create your first session above."));
        table.setPrefHeight(270);
        table.getColumns().addAll(
                column("Start", 180, s -> DISPLAY.format(s.startTime())),
                column("End", 180, s -> DISPLAY.format(s.endTime())),
                column("Subject", 160, SessionSlot::courseTitle),
                column("Status", 110, SessionSlot::status));

        Label status = message();
        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(
                service.mentorSessions(mentor)));

        Button create = new Button("Publish session");
        create.setOnAction(e -> execute(status, () -> {
            LocalDateTime begin = selectedStart(date, start);
            service.createSession(mentor, begin, begin.plusMinutes(requiredDuration(minutes)));
            reload.run();
        }, "Session published."));

        Button edit = new Button("Update selected");
        edit.setOnAction(e -> execute(status, () -> {
            SessionSlot selected = selected(table);
            LocalDateTime begin = selectedStart(date, start);
            service.editSession(mentor, selected.id(), begin, begin.plusMinutes(requiredDuration(minutes)));
            reload.run();
        }, "Session updated."));

        Button remove = new Button("Remove selected");
        remove.setOnAction(e -> execute(status, () -> {
            service.removeSession(mentor, selected(table).id());
            reload.run();
        }, "Session removed."));

        Button complete = new Button("Mark completed");
        complete.setOnAction(e -> execute(status, () -> {
            service.completeSession(mentor, selected(table).id());
            reload.run();
        }, "Session marked completed. Feedback is now available after the Feedback tab reloads."));

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> execute(status, reload, "Session list refreshed."));

        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, current) -> {
            if (current == null) return;
            date.setValue(current.startTime().toLocalDate());
            start.setValue(current.startTime().toLocalTime());
            long duration = java.time.Duration.between(current.startTime(), current.endTime()).toMinutes();
            if (minutes.getItems().contains((int) duration)) minutes.setValue((int) duration);
        });

        try { reload.run(); } catch (RuntimeException ex) { showError(status, ex); }
        return new VBox(12, title, instruction, form, create,
                new Label("Your sessions"), table,
                new HBox(10, edit, remove, complete, refresh), status);
    }

    private VBox menteeView(User mentee) {
        Label title = header("Book sessions");
        Label info = new Label("Select an available slot and book it, or cancel an upcoming booking.");
        info.setWrapText(true);
        TableView<SessionSlot> available = new TableView<>();
        available.setPrefHeight(260);
        available.setPlaceholder(new Label("There are no available mentorship sessions."));
        available.getColumns().addAll(
                column("Mentor", 150, SessionSlot::mentorName),
                column("Start", 180, s -> DISPLAY.format(s.startTime())),
                column("End", 180, s -> DISPLAY.format(s.endTime())),
                column("Subject", 150, SessionSlot::courseTitle));

        TableView<SessionBooking> bookings = new TableView<>();
        bookings.setPrefHeight(220);
        bookings.setPlaceholder(new Label("You have no bookings."));
        bookings.getColumns().addAll(
                column("Mentor", 150, SessionBooking::mentorName),
                column("Start", 180, b -> DISPLAY.format(b.startTime())),
                column("End", 180, b -> DISPLAY.format(b.endTime())),
                column("Status", 110, SessionBooking::status));

        Label status = message();
        Runnable reload = () -> {
            available.setItems(FXCollections.observableArrayList(service.availableSessions(mentee)));
            bookings.setItems(FXCollections.observableArrayList(service.myBookings(mentee)));
        };
        Button book = new Button("Register for selected session");
        book.setOnAction(e -> execute(status, () -> {
            service.book(mentee, selected(available).id());
            reload.run();
        }, "Session booked successfully."));
        Button cancel = new Button("Cancel selected booking");
        cancel.setOnAction(e -> execute(status, () -> {
            service.cancel(mentee, selected(bookings).id());
            reload.run();
        }, "Booking cancelled. The session is available again."));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> execute(status, reload, "Session lists refreshed."));

        try { reload.run(); } catch (RuntimeException ex) { showError(status, ex); }
        return new VBox(12, title, info,
                new Label("Available mentorship sessions"), available, book,
                new Label("My bookings"), bookings,
                new HBox(10, cancel, refresh), status);
    }

    private static ComboBox<LocalTime> startTimes() {
        ComboBox<LocalTime> times = new ComboBox<>();
        for (int hour = 0; hour <= 23; hour++) {
            times.getItems().add(LocalTime.of(hour, 0));
            times.getItems().add(LocalTime.of(hour, 30));
        }
        times.setValue(LocalTime.of(10, 0));
        return times;
    }

    private static ComboBox<Integer> durations() {
        ComboBox<Integer> durations = new ComboBox<>();
        durations.getItems().addAll(30, 60, 90, 120);
        durations.setValue(60);
        return durations;
    }

    private static LocalDateTime selectedStart(DatePicker date, ComboBox<LocalTime> start) {
        if (date.getValue() == null || start.getValue() == null) {
            throw new IllegalArgumentException("Choose a date and a start time.");
        }
        return LocalDateTime.of(date.getValue(), start.getValue());
    }

    private static int requiredDuration(ComboBox<Integer> minutes) {
        if (minutes.getValue() == null) throw new IllegalArgumentException("Choose a session duration.");
        return minutes.getValue();
    }

    private static <T> T selected(TableView<T> table) {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null) throw new IllegalArgumentException("Select a session or booking first.");
        return item;
    }

    private static Label header(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        return label;
    }

    private static Label message() {
        Label label = new Label();
        label.setWrapText(true);
        return label;
    }

    private static void execute(Label label, Runnable action, String success) {
        try {
            action.run();
            label.setStyle("-fx-text-fill: #1e8449;");
            label.setText(success);
        } catch (RuntimeException ex) { showError(label, ex); }
    }

    private static void showError(Label label, RuntimeException ex) {
        label.setStyle("-fx-text-fill: #c0392b;");
        label.setText(ex.getMessage() == null ? "Unable to complete the operation." : ex.getMessage());
    }

    private static <T> TableColumn<T, String> column(String title, int width, Function<T, String> value) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(d -> new SimpleStringProperty(value.apply(d.getValue())));
        return col;
    }
}
