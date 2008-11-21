package org.apache.maven.project.builder.profile;

import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelDataSource;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.project.builder.ProjectUri;

import java.util.*;


public class ProfileContext {

    private ModelDataSource modelDataSource;

    private List<InterpolatorProperty> properties;

    List<ActiveProfileMatcher> matchers  = Collections.unmodifiableList( Arrays.asList(new ByDefaultMatcher(),
            new FileMatcher(), new JdkMatcher(), new OperatingSystemMatcher(), new PropertyMatcher()
         ) );

    public ProfileContext(ModelDataSource modelDataSource, List<InterpolatorProperty> properties) {
        this.modelDataSource = modelDataSource;
        this.properties = new ArrayList<InterpolatorProperty>(properties);
    }

    public Collection<ModelContainer> getActiveProfiles() throws DataSourceException {
        List<ModelContainer> matchedContainers = new ArrayList<ModelContainer>();

        List<ModelContainer> modelContainers  = modelDataSource.queryFor(ProjectUri.Profiles.Profile.xUri);
        for(ModelContainer mc : modelContainers) {
            for(ActiveProfileMatcher matcher : matchers) {
                if(matcher.isMatch(mc, properties)) {
                    matchedContainers.add(mc);
                }
            }
        }

        return matchedContainers;       
    }
}
