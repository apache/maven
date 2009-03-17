package org.apache.maven.profiles.matchers;

import java.util.List;

import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.model.Profile;

public interface ProfileMatcher
{
    boolean isMatch( Profile profile, List<InterpolatorProperty> properties );
}
