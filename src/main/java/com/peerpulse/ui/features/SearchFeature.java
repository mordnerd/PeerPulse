package com.peerpulse.ui.features;

import com.peerpulse.model.Course;
import com.peerpulse.model.PeerMatch;
import com.peerpulse.model.Role;
import com.peerpulse.model.User;
import com.peerpulse.service.SearchService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SearchFeature implements FeatureTab {
    private static final Course ANY_COURSE = new Course(0, "Any course", "", "");
    private static final String ANY_SKILL = "Any skill";

    private final SearchService service = new SearchService();

    @Override
    public String title() {
        return "Search";
    }

    @Override
    public boolean appliesTo(Role role) {
        return role == Role.MENTOR || role == Role.MENTEE;
    }

    @Override
    public Parent build(User user) {
        boolean lookingForMentors = user.role() == Role.MENTEE;
        Label heading = new Label(lookingForMentors ? "Find mentors" : "Find mentees");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField query = new TextField();
        query.setPromptText("Course code / title / skill");
        HBox.setHgrow(query, Priority.ALWAYS);
        Button search = new Button("Search");
        search.setDefaultButton(true);

        ComboBox<Course> courseFilter = new ComboBox<>();
        courseFilter.getItems().add(ANY_COURSE);
        courseFilter.getItems().addAll(service.courseFilters(user));
        courseFilter.setValue(ANY_COURSE);
        courseFilter.setPrefWidth(220);
        courseFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Course course) {
                if (course == null || course.id() == 0) {
                    return "Any course";
                }
                return course.code() + " — " + course.title();
            }

            @Override
            public Course fromString(String string) {
                return ANY_COURSE;
            }
        });

        ComboBox<String> skillFilter = new ComboBox<>();
        skillFilter.getItems().add(ANY_SKILL);
        skillFilter.getItems().addAll(service.skillFilters(user));
        skillFilter.setValue(ANY_SKILL);
        skillFilter.setPrefWidth(160);

        Label resultCount = new Label("Search to see matching peers.");
        ListView<PeerMatch> results = new ListView<>();
        results.setPlaceholder(new Label(SearchService.EMPTY_RESULTS));
        results.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PeerMatch item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.fullName() + "  ·  "
                        + (item.courses().isBlank() ? item.skills() : item.courses()));
            }
        });

        TextArea details = new TextArea("Select a peer, then view details.");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(7);
        details.setMinHeight(130);
        Button viewDetails = new Button("View peer details");
        viewDetails.setOnAction(e -> {
            PeerMatch selected = results.getSelectionModel().getSelectedItem();
            if (selected == null) {
                details.setText("Select a peer first.");
                return;
            }
            details.setText(detailsText(selected));
        });

        Runnable runSearch = () -> {
            Course selectedCourse = courseFilter.getValue();
            Integer courseId = selectedCourse == null || selectedCourse.id() == 0 ? null : selectedCourse.id();
            String skill = ANY_SKILL.equals(skillFilter.getValue()) ? "" : skillFilter.getValue();
            try {
                var matches = service.search(user, query.getText(), courseId, skill);
                results.setItems(FXCollections.observableArrayList(matches));
                resultCount.setText(SearchService.resultSummary(matches.size()));
                details.setText(matches.isEmpty()
                        ? SearchService.EMPTY_RESULTS
                        : "Select a peer, then view details.");
            } catch (RuntimeException ex) {
                resultCount.setText(ex.getMessage());
            }
        };
        search.setOnAction(e -> runSearch.run());

        HBox searchRow = new HBox(8, query, search);
        HBox filters = new HBox(8, courseFilter, skillFilter);
        results.setPrefHeight(180);
        results.setMinHeight(120);
        VBox content = new VBox(10, heading, searchRow, filters, resultCount, results, viewDetails, details);
        content.setPadding(new Insets(20));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private static String detailsText(PeerMatch peer) {
        String courses = peer.courses().isBlank() ? "None listed" : peer.courses();
        return peer.fullName()
                + "\nRole: " + peer.role()
                + "\nAcademic year: " + blank(peer.academicYear())
                + "\nMajor: " + blank(peer.major())
                + "\nTechnical skills: " + blank(peer.skills())
                + "\nCourses: " + courses;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
