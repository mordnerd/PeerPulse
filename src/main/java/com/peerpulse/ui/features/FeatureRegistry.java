package com.peerpulse.ui.features;

import java.util.List;

public final class FeatureRegistry {
    private static final List<FeatureTab> FEATURES = List.of(
            new ProfileFeature(),
            new SearchFeature(),
            new CoursesFeature(),
            new FeedbackFeature()
    );

    private FeatureRegistry() {}

    public static List<FeatureTab> all() {
        return FEATURES;
    }
}
