package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.List;

public class OperatingSystemMatcher implements ActiveProfileMatcher {

    public boolean isMatch(ModelContainer modelContainer, List<InterpolatorProperty> properties) {
        if(modelContainer == null ) {
            throw new IllegalArgumentException("modelContainer: null");
        }

        if(!doTest(modelContainer)) {
            return false;
        }

        for(InterpolatorProperty property : properties) {
            if(!matches(modelContainer, property)) {
                return false;
            }
        }

        return true;
    }

    private static boolean doTest(ModelContainer modelContainer) {
        for(ModelProperty mp : modelContainer.getProperties()) {
            if(mp.getUri().startsWith(ProjectUri.Profiles.Profile.Activation.Os.xUri)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ModelContainer modelContainer, InterpolatorProperty interpolatorProperty) {
        String key = interpolatorProperty.getKey();

        for(ModelProperty property : modelContainer.getProperties()) {
            if((key.equals("${os.arch}") && property.getUri().equals(ProjectUri.Profiles.Profile.Activation.Os.arch))
                    || (key.equals("${os.version}") && property.getUri().equals(ProjectUri.Profiles.Profile.Activation.Os.version))
                    || (key.equals("${os.family}") && property.getUri().equals(ProjectUri.Profiles.Profile.Activation.Os.family))
                    || (key.equals("${os.name}") && property.getUri().equals(ProjectUri.Profiles.Profile.Activation.Os.name)) )
            {

                if(property.getResolvedValue().startsWith("!"))
                {
                    return !interpolatorProperty.getValue().equals(property.getResolvedValue());
                }
                else
                {
                    return interpolatorProperty.getValue().equals(property.getResolvedValue());    
                }

            }
        }
        return true;
    }
}
