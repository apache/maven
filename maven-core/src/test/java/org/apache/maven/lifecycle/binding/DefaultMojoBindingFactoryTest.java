package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultMojoBindingFactoryTest
    extends PlexusTestCase
{

    private MojoBindingFactory factory;

    public void setUp() throws Exception
    {
        super.setUp();

        factory = (MojoBindingFactory) lookup( MojoBindingFactory.ROLE, "default" );
    }

    public void testPrefixGoalSpec_PrefixReferenceNotAllowed()
        throws LifecycleLoaderException
    {
        String spec = "prefix:goal";

        try
        {
            factory.parseMojoBinding( spec, new MavenProject( new Model() ), null, false );

            fail( "Should fail when prefix references are not allowed." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected.
        }
    }

    public void testGroupIdArtifactIdGoalSpec_ShouldParseCorrectly()
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        String spec = "group:artifact:goal";

        MojoBinding binding = factory.parseMojoBinding( spec, new MavenProject( new Model() ), null, false );

        assertEquals( "group", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertNull( binding.getVersion() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testGroupIdArtifactIdVersionGoalSpec_ShouldParseCorrectly()
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        String spec = "group:artifact:version:goal";

        MojoBinding binding = factory.parseMojoBinding( spec, new MavenProject( new Model() ), null, false );

        assertEquals( "group", binding.getGroupId() );
        assertEquals( "artifact", binding.getArtifactId() );
        assertEquals( "version", binding.getVersion() );
        assertEquals( "goal", binding.getGoal() );
    }

    public void testSpecWithTooManyParts_ShouldFail()
        throws LifecycleLoaderException
    {
        String spec = "group:artifact:version:type:goal";

        try
        {
            factory.parseMojoBinding( spec, new MavenProject( new Model() ), null, false );

            fail( "Should fail because spec has too many parts (type part is not allowed)." );
        }
        catch ( LifecycleSpecificationException e )
        {
            // expected
        }
    }

}
