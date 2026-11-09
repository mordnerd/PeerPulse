package com.peerpulse.ui.features;

import com.peerpulse.model.*;
import javafx.scene.Parent;

/** One feature screen. Each feature implements this and is listed in FeatureRegistry. */
public interface FeatureTab {
    String title();
    boolean appliesTo(Role role);
    Parent build(User user);
}