package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;

public class ByDefaultMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {
        if(modelContainer == null ) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        for(ModelProperty mp : modelContainer.getProperties()) {
            if(mp.getUri().equals(ProjectUri.Profiles.Profile.Activation.activeByDefault)) {
               return true;
            }
        }
        return false;
    }
}
