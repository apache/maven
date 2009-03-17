package org.apache.maven.profiles.matchers;

import java.util.List;

import org.apache.maven.model.Profile;
import org.apache.maven.shared.model.InterpolatorProperty;

public class PropertyMatcher implements ProfileMatcher
{
    public boolean isMatch( Profile profile, List<InterpolatorProperty> properties )
    {
        if (profile == null) {
            throw new IllegalArgumentException("profile: null");
        }

        if(profile.getActivation() == null || profile.getActivation().getProperty() == null)
        {
            return false;
        }
        String value = profile.getActivation().getProperty().getValue();
        String name =  profile.getActivation().getProperty().getName();

        if(name == null )
        {
            return false;
        }

        if(value == null)
        {
            return !name.startsWith("!");
        }

        for(InterpolatorProperty ip : properties) {
            if(ip.getKey().equals("${" + name + "}")) {
                return ip.getValue().equals(value);
            }
        }

        return false;
    }
}
