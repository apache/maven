package org.apache.maven.embedder;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;

import junit.framework.TestCase;

/** @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a> */
public class TestComponentOverride
    extends TestCase
{
    private String basedir;

    private MavenEmbedder maven;

    protected PlexusContainer container;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        MavenEmbedderConfiguration request = new DefaultMavenEmbedderConfiguration();

        request.setClassLoader( loader );

        request.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        request.addExtension( new File( basedir, "src/test/extensions" ).toURI().toURL() );

        // register callback to get a hold of the container
        request.setConfigurationCustomizer( new ContainerCustomizer()
        {
            public void customize( PlexusContainer container )
            {
                TestComponentOverride.this.container = container;
            }
        } );

        maven = new MavenEmbedder( request );
    }

    public void testComponentOverride()
        throws ComponentLookupException
    {
        ArtifactFactory factory = (ArtifactFactory) container.lookup( ArtifactFactory.class );

        assertNotNull( factory );

        assertTrue( "Expecting " + CustomArtifactFactory.class.getName() + " but was " + factory.getClass().getName(),
                    CustomArtifactFactory.class.isAssignableFrom( factory.getClass() ) );

        // test wheter the requirement is injected - if not, it nullpointers
        factory.createArtifact( "testGroupId", "testArtifactId", "testVersion", "compile", "jar" );
    }
}
