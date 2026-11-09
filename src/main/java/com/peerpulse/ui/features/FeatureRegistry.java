package com.peerpulse.ui.features;

import java.util.List;

public final class FeatureRegistry {
    private static final List<FeatureTab> FEATURES = List.of(
            new ProfileFeature()
            // Future features go here, e.g. new CourseFeature(), new SchedulingFeature()
    );

    private FeatureRegistry() {}

    public static List<FeatureTab> all() {
        return FEATURES;
    }
}