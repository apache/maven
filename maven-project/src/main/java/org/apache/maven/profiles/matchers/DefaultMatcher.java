package org.apache.maven.profiles.matchers;

import java.util.List;

import org.apache.maven.model.Profile;
import org.apache.maven.shared.model.InterpolatorProperty;

public class DefaultMatcher implements ProfileMatcher
{
    public boolean isMatch( Profile profile, List<InterpolatorProperty> properties )
    {
        if(profile.getActivation() == null)
        {
            return false;
        }
        return profile.getActivation().isActiveByDefault();
    }

}
