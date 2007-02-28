package org.apache.maven.embedder;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Arrays;

public class MavenEmbedderCrappySettingsConfigurationTest
    extends PlexusTestCase
{
    public void testEmbedderWillStillStartupWhenTheSettingsConfigurationIsCrap()
        throws Exception
    {
        // START SNIPPET: simple-embedder-example

        File projectDirectory = new File( getBasedir(), "src/examples/simple-project" );

        File user = new File( projectDirectory, "invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user )
            .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

        assertFalse( validationResult.isValid() );

        MavenEmbedder embedder = new MavenEmbedder( configuration );

        assertNotNull( embedder.getLocalRepository().getBasedir() );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( projectDirectory )
            .setGoals( Arrays.asList( new String[]{"clean", "install"} ) );

        MavenExecutionResult result = embedder.execute( request );

        assertNotNull( result.getMavenProject() );

        MavenProject project = result.getMavenProject();

        String environment = project.getProperties().getProperty( "environment" );

        assertEquals( "development", environment );

        // END SNIPPET: simple-embedder-example
    }
}
