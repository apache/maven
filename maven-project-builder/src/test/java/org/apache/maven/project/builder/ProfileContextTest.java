package org.apache.maven.project.builder;

import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProfileContextTest {

    @org.junit.Test
    public void getActiveProfiles() throws DataSourceException {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.name , "foo"));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.value , "bar"));

        DefaultModelDataSource dataSource = new DefaultModelDataSource();
        dataSource.init(modelProperties, Arrays.asList(new ArtifactModelContainerFactory(), new IdModelContainerFactory()));

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.add(new InterpolatorProperty( "${foo}", "bar"));

        ProfileContext ctx = new ProfileContext(dataSource, interpolatorProperties);

        Collection<ModelContainer> profiles = ctx.getActiveProfiles();

        assertTrue(profiles.size() == 1);

    }
}
