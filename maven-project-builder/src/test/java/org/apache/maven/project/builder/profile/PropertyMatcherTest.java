package org.apache.maven.project.builder.profile;

import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;


public class PropertyMatcherTest {

    @org.junit.Test
    public void propertyMatches()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.name , "foo"));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.value , "bar"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${foo}" , "bar"));

        PropertyMatcher matcher = new PropertyMatcher();
        assertTrue(matcher.isMatch(modelContainer, props));
    }

    @org.junit.Test
    public void propertyDoesNotMatch()  {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.name , "foo"));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.value , "bars"));

        ModelContainer modelContainer = new DefaultModelContainer(modelProperties);

        List<InterpolatorProperty> props = new ArrayList<InterpolatorProperty>();
        props.add(new InterpolatorProperty("${foo}" , "bar"));

        PropertyMatcher matcher = new PropertyMatcher();
        assertFalse(matcher.isMatch(modelContainer, props));
    }

}
