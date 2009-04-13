package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class FileMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {
        if(modelContainer == null ) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        List<String> exists = new ArrayList<String>(), missings = new ArrayList<String>();

        for(ModelProperty mp : modelContainer.getProperties()) {
            if(mp.getUri().equals(ProjectUri.Profiles.Profile.Activation.File.exists)) {
                exists.add(mp.getValue());
            } else if(mp.getUri().equals(ProjectUri.Profiles.Profile.Activation.File.missing)) {
                missings.add(mp.getValue());
            }
        }

        if(exists.isEmpty() && missings.isEmpty()) {
            return false;
        }

        for(String exist : exists) {
            if(!new File(exist).exists()) {
                return false;
            }
        }

         for(String missing : missings) {
            if(new File(missing).exists()) {
                return false;
            }
        }

        return true;
    }
}
