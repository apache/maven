package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;

import java.util.List;

public interface ActiveProfileMatcher {

    /**
     * If model container does not contain the activator property, must return false.
     *
     * @param modelContainer
     * @param properties
     * @return
     */
    boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties);
}
