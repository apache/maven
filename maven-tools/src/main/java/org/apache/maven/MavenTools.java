package org.apache.maven;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface MavenTools
{
    String ROLE = MavenTools.class.getName();

    String userHome = System.getProperty( "user.home" );

    File userMavenConfigurationHome = new File( userHome, ".m2" );

    String mavenHome = System.getProperty( "maven.home" );

    // ----------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------

    File defaultUserSettingsFile = new File( userMavenConfigurationHome, "settings.xml" );

    File defaultGlobalSettingsFile = new File( mavenHome, "conf/settings.xml" );

    String ALT_USER_SETTINGS_XML_LOCATION = "org.apache.maven.user-settings";

    String ALT_GLOBAL_SETTINGS_XML_LOCATION = "org.apache.maven.global-settings";

    // ----------------------------------------------------------------------
    // Local Repository
    // ----------------------------------------------------------------------

    String ALT_LOCAL_REPOSITORY_LOCATION = "maven.repo.local";

    File defaultUserLocalRepository = new File( userMavenConfigurationHome, "repository" );

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

    ArtifactRepository createDefaultLocalRepository()
        throws SettingsConfigurationException;

    ArtifactRepository createLocalRepository( File localRepositoryPath );

    Settings buildSettings( File userSettingsPath,
                            File globalSettingsPath,
                            boolean interactive,
                            boolean offline,
                            boolean usePluginRegistry,
                            boolean pluginUpdateOverride )
        throws SettingsConfigurationException;
    
    Settings buildSettings( File userSettingsPath,
                            File globalSettingsPath,
                            boolean pluginUpdateOverride )
        throws SettingsConfigurationException;

    // ----------------------------------------------------------------------------
    // Methods taken from CLI
    // ----------------------------------------------------------------------------

    File getUserSettingsPath( String optionalSettingsPath );

    File getGlobalSettingsPath();

    String getLocalRepositoryPath( Settings settings );

    String getLocalRepositoryPath()
        throws SettingsConfigurationException;

    // ----------------------------------------------------------------------------
    // Methods taken from ProjectUtils
    // ----------------------------------------------------------------------------

    List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException;

    ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException;

    ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException;

}
