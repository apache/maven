package org.apache.maven.embedder;

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/** @author Jason van Zyl */
public class MavenEmbedderBehaviorTest
    extends PlexusTestCase
{
    public void testThatTheLocalRepositoryIsTakenFromGlobalSettingsWhenUserSettingsAreNull()
        throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration()
            .setClassLoader( classLoader )
            .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() )
            .setUserSettingsFile( null )
            .setGlobalSettingsFile( new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" ) );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isValid() );

        MavenEmbedder maven = new MavenEmbedder( configuration );

        assertEquals( "/global/maven/local-repository", maven.getLocalRepository().getBasedir() );

        maven.stop();
    }
}
