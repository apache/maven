package org.apache.maven.project.builder.profile;

import org.apache.maven.project.builder.profile.ProfileContext;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.DataSourceException;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.impl.DefaultModelDataSource;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProfileContextTest {

    @Test
    public void getActiveProfiles() throws DataSourceException {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.xUri , null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.name , "foo"));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.Activation.Property.value , "bar"));

        DefaultModelDataSource dataSource = new DefaultModelDataSource(modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.add(new InterpolatorProperty( "${foo}", "bar"));

        ProfileContext ctx = new ProfileContext(dataSource, null, null, interpolatorProperties);

        Collection<ModelContainer> profiles = ctx.getActiveProfiles();

        assertTrue(profiles.size() == 1);

    }

    @Test
    public void getActiveProfilesById() throws DataSourceException {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add(new ModelProperty(ProjectUri.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.xUri, null));
        modelProperties.add(new ModelProperty(ProjectUri.Profiles.Profile.id , "test"));

        DefaultModelDataSource dataSource = new DefaultModelDataSource(modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();

        ProfileContext ctx = new ProfileContext(dataSource, Arrays.asList("test"), null, interpolatorProperties);

        Collection<ModelContainer> profiles = ctx.getActiveProfiles();

        assertTrue(profiles.size() == 1);

    }

    @Test
    public void getActiveByDefaultProfilesOnlyActivatedIfNoOtherPomProfilesAreActive()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( ProjectUri.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.id, "default" ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.Activation.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.Activation.activeByDefault, "true" ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.id, "explicit" ) );

        DefaultModelDataSource dataSource =
            new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();

        ProfileContext ctx = new ProfileContext( dataSource, Arrays.asList( "explicit" ), null, interpolatorProperties );

        Collection<ModelContainer> profiles = ctx.getActiveProfiles();

        assertEquals( 1, profiles.size() );
        assertProperty( profiles.iterator().next().getProperties(), ProjectUri.Profiles.Profile.id, "explicit" );
    }

    @Test
    public void getDeactivateProfiles()
        throws DataSourceException
    {
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( ProjectUri.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.id, "default" ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.Activation.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Profiles.Profile.Activation.activeByDefault, "true" ) );

        DefaultModelDataSource dataSource =
            new DefaultModelDataSource( modelProperties, PomTransformer.MODEL_CONTAINER_FACTORIES );

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();

        ProfileContext ctx = new ProfileContext( dataSource, null, Arrays.asList( "default" ), interpolatorProperties );

        Collection<ModelContainer> profiles = ctx.getActiveProfiles();

        assertEquals( 0, profiles.size() );
    }

    private void assertProperty( Collection<ModelProperty> properties, String uri, String value )
    {
        for ( ModelProperty property : properties )
        {
            if ( uri.equals( property.getUri() ) && value.equals( property.getValue() ) )
            {
                return;
            }
        }
        fail( "missing model property " + uri + " = " + value );
    }

}
