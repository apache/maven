package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;

public class PropertyMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {

        if (modelContainer == null) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        String name = null, value = null;

        for(ModelProperty mp : modelContainer.getProperties()) {
            if(mp.getUri().equals(ProjectUri.Profiles.Profile.Activation.Property.name)) {
                name = mp.getValue();
            } else if(mp.getUri().equals(ProjectUri.Profiles.Profile.Activation.Property.value)) {
                value = mp.getValue();
            }
        }

        if(name == null || value == null) {
            return false;
        }

        for(InterpolatorProperty ip : properties) {
            if(ip.getKey().equals("${" + name + "}")) {
                return ip.getValue().equals(value);
            }
        }

        return false;
    }
}
