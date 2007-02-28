package org.apache.maven.embedder;

import org.codehaus.plexus.PlexusTestCase;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.Configuration;

import java.io.File;

/** @author Jason van Zyl */
public class ValidateConfigurationTest
    extends PlexusTestCase
{
    public void testConfigurationOnlyUserSettingsAreActiveAndItIsValid()
    {
        File user = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isUserSettingsFilePresent() );

        assertTrue( result.isUserSettingsFileParses() );
    }

    public void testConfigurationOnlyUserSettingsAreActiveAndItIsInvalid()
    {
        File user = new File( getBasedir(), "src/test/resources/settings/invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isUserSettingsFilePresent() );

        assertFalse( result.isUserSettingsFileParses() );
    }

    public void testConfigurationOnlyGlobalSettingsAreActiveAndItIsValid()
    {
        File global = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( global );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isGlobalSettingsFilePresent() );

        assertTrue( result.isGlobalSettingsFileParses() );
    }

    public void testConfigurationOnlyGlobalSettingsAreActiveAndItIsInvalid()
    {
        File global = new File( getBasedir(), "src/test/resources/settings/invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setGlobalSettingsFile( global );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isGlobalSettingsFilePresent() );

        assertFalse( result.isGlobalSettingsFileParses() );
    }
}
