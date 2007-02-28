package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;

public class MavenEmbedderExampleTest
    extends PlexusTestCase
{
    public void testEmbedderExample()
        throws Exception
    {
        // START SNIPPET: simple-embedder-example

        File projectDirectory = new File( getBasedir(), "examples/simple-project" );

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
        }

        // END SNIPPET: simple-embedder-example
    }
}
