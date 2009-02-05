package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.*;
import org.apache.maven.project.builder.ProjectUri;

import java.util.*;


public class ProfileContext {

    private ModelDataSource modelDataSource;

    private List<InterpolatorProperty> properties;

    private Collection<String> activeProfileIds;

    List<ActiveProfileMatcher> matchers  = Collections.unmodifiableList( Arrays.asList(new ByDefaultMatcher(),
            new FileMatcher(), new JdkMatcher(), new OperatingSystemMatcher(), new PropertyMatcher()
         ) );

    public ProfileContext(ModelDataSource modelDataSource, Collection<String> activeProfileIds,
                          List<InterpolatorProperty> properties) {
        this.modelDataSource = modelDataSource;
        this.properties = new ArrayList<InterpolatorProperty>(properties);
        this.activeProfileIds = (activeProfileIds != null) ? activeProfileIds : new ArrayList<String>();
    }

    public Collection<ModelContainer> getActiveProfiles() throws DataSourceException {
        List<ModelContainer> matchedContainers = new ArrayList<ModelContainer>();

        List<ModelContainer> modelContainers  = modelDataSource.queryFor(ProjectUri.Profiles.Profile.xUri);
        for(ModelContainer mc : modelContainers) {
            for(ActiveProfileMatcher matcher : matchers) {
                if(matcher.isMatch(mc, properties)) {
                    matchedContainers.add(mc);
                    continue;
                }
            }

            String profileId = getProfileId(mc.getProperties());
            if(profileId != null && activeProfileIds.contains(profileId))
            {
                matchedContainers.add(mc);
            }
        }

        return matchedContainers;       
    }

    private String getProfileId(List<ModelProperty> modelProperties)
    {
        for(ModelProperty mp : modelProperties)
        {
            if(mp.getUri().equals(ProfileUri.Profiles.Profile.id))
            {
                return mp.getResolvedValue();
            }
        }
        return null;
    }
}
