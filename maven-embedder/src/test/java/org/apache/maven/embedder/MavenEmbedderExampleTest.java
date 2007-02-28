package org.apache.maven.embedder;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.PlexusContainer;

import java.io.File;
import java.util.Arrays;

public class MavenEmbedderExampleTest
    extends PlexusTestCase
{
    public void testEmbedderExample()
        throws Exception
    {
        // START SNIPPET: simple-embedder-example

        File projectDirectory = new File( getBasedir(), "src/examples/simple-project" );

        File user = new File( projectDirectory, "settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user )
            .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

        if ( validationResult.isValid() )
        {
            MavenEmbedder embedder = new MavenEmbedder( configuration );

            MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setBaseDirectory( projectDirectory )
                .setGoals( Arrays.asList( new String[]{"clean", "install"} ) );

            MavenExecutionResult result = embedder.execute( request );

            if ( result.hasExceptions() )
            {
                // Notify user that exceptions have occured.
            }

            // ----------------------------------------------------------------------------
            // You may want to inspect the project after the execution.
            // ----------------------------------------------------------------------------

            MavenProject project = result.getMavenProject();

            // Do something with the project

            String groupId = project.getGroupId();

            String artifactId = project.getArtifactId();

            String version = project.getVersion();

            String name = project.getName();

            String environment = project.getProperties().getProperty( "environment" );

            assertEquals( "development", environment );

            System.out.println( "You are working in the '" + environment + "' environment!" );
        }
        else
        {
            if ( ! validationResult.isUserSettingsFilePresent() )
            {
                System.out.println( "The specific user settings file '" + user + "' is not present." );
            }
            else if ( ! validationResult.isUserSettingsFileParses() )
            {
                System.out.println( "Please check your settings file, it is not well formed XML." );
            }
        }

        // END SNIPPET: simple-embedder-example
    }

    public void testEmbedderExampleThatShowsHowToMimicTheMavenCLI()
        throws Exception
    {

        // START SNIPPET: mimic-cli
        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE )
            .setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE )
            .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

        if ( validationResult.isValid() )
        {
            // If the configuration is valid then do your thang ...
        }
        // END SNIPPET: mimic-cli
    }

    public void testEmbedderExampleThatShowsAccessingThePlexusContainer()
        throws Exception
    {

        // START SNIPPET: plexus-container
        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE )
            .setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE )
            .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

        if ( validationResult.isValid() )
        {
            // If the configuration is valid then do your thang ...
        }

        MavenEmbedder embedder = new MavenEmbedder( configuration );

        PlexusContainer container = embedder.getPlexusContainer();

        // Do what you like with the container ...

        // END SNIPPET: plexus-container
    }

}
