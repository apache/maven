package org.apache.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.apache.maven.model.Repository;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.RepositoryBase;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Jason van Zyl
 */
public class DefaultMavenTools
    implements MavenTools,
    Contextualizable
{
    private ArtifactRepositoryLayout repositoryLayout;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    private PlexusContainer container;

    // ----------------------------------------------------------------------------
    // ArtifactRepository
    // ----------------------------------------------------------------------------

    public ArtifactRepository createDefaultLocalRepository()
        throws SettingsConfigurationException
    {
        return createLocalRepository( new File( getLocalRepositoryPath() ) );
    }

    public ArtifactRepository createLocalRepository( File directory )
    {
        String localRepositoryUrl = directory.getAbsolutePath();

        if ( !localRepositoryUrl.startsWith( "file:" ) )
        {
            localRepositoryUrl = "file://" + localRepositoryUrl;
        }

        return createRepository( "local", localRepositoryUrl);
    }

    private ArtifactRepository createRepository( String repositoryId,
                                                String repositoryUrl)
    {
        ArtifactRepository localRepository =
            new DefaultArtifactRepository( repositoryId, repositoryUrl, repositoryLayout );
        return localRepository;
    }

    // ----------------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------------

    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean interactive,
                                   boolean offline,
                                   boolean usePluginRegistry,
                                   boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        Settings settings = buildSettings(userSettingsPath,
                                          globalSettingsPath,
                                          pluginUpdateOverride);
        if ( offline )
        {
            settings.setOffline( true );
        }
        
        settings.setInteractiveMode( interactive );
        
        settings.setUsePluginRegistry( usePluginRegistry );
        
        return settings;
    }
    
    public Settings buildSettings( File userSettingsPath,
                                   File globalSettingsPath,
                                   boolean pluginUpdateOverride )
        throws SettingsConfigurationException
    {
        Settings settings;

        try
        {
            settings = settingsBuilder.buildSettings( userSettingsPath, globalSettingsPath );
        }
        catch ( IOException e )
        {
            throw new SettingsConfigurationException( "Error reading settings file", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new SettingsConfigurationException( e.getMessage(), e.getDetail(), e.getLineNumber(),
                                                      e.getColumnNumber() );
        }

        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        runtimeInfo.setPluginUpdateOverride( Boolean.valueOf( pluginUpdateOverride ) );

        settings.setRuntimeInfo( runtimeInfo );

        return settings;
    }

    // ----------------------------------------------------------------------------
    // Code snagged from ProjectUtils: this will have to be moved somewhere else
    // but just trying to collect it all in one place right now.
    // ----------------------------------------------------------------------------

    public List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException
    {

        List repos = new ArrayList();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            Repository mavenRepo = (Repository) i.next();

            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }
        return repos;
    }

    public ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo );

            return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, layout,
                                                                                 repo.isUniqueVersion() );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();

            // TODO: make this a map inside the factory instead, so no lookup needed
            ArtifactRepositoryLayout layout = getRepositoryLayout( repo );

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );
            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, layout, snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    public ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
    {
        boolean enabled = true;
        String updatePolicy = null;
        String checksumPolicy = null;

        if ( policy != null )
        {
            enabled = policy.isEnabled();
            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }

        return new ArtifactRepositoryPolicy( enabled, updatePolicy, checksumPolicy );
    }

    private ArtifactRepositoryLayout getRepositoryLayout( RepositoryBase mavenRepo )
        throws InvalidRepositoryException
    {
        String layout = mavenRepo.getLayout();

        ArtifactRepositoryLayout repositoryLayout;
        try
        {
            repositoryLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidRepositoryException( "Cannot find layout implementation corresponding to: \'" + layout +
                "\' for remote repository with id: \'" + mavenRepo.getId() + "\'.", e );
        }
        return repositoryLayout;
    }

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

    /**
     * Retrieve the user settings path using the followiwin search pattern:
     * <p/>
     * 1. System Property
     * 2. Optional path
     * 3. ${user.home}/.m2/settings.xml
     */
    public File getUserSettingsPath( String optionalSettingsPath )
    {
        File userSettingsPath = new File( System.getProperty( ALT_USER_SETTINGS_XML_LOCATION ) + "" );

        if ( !userSettingsPath.exists() )
        {
            if ( optionalSettingsPath != null )
            {
                File optionalSettingsPathFile = new File( optionalSettingsPath );

                if ( optionalSettingsPathFile.exists() )
                {
                    userSettingsPath = optionalSettingsPathFile;
                }
                else
                {
                    userSettingsPath = defaultUserSettingsFile;
                }
            }
            else
            {
                userSettingsPath = defaultUserSettingsFile;
            }
        }

        return userSettingsPath;
    }

    /**
     * Retrieve the global settings path using the followiwin search pattern:
     * <p/>
     * 1. System Property
     * 2. CLI Option
     * 3. ${maven.home}/conf/settings.xml
     */
    public File getGlobalSettingsPath()
    {
        File globalSettingsFile = new File( System.getProperty( ALT_GLOBAL_SETTINGS_XML_LOCATION ) + "" );

        if ( !globalSettingsFile.exists() )
        {
            globalSettingsFile = defaultGlobalSettingsFile;
        }

        return globalSettingsFile;
    }

    /**
     * Retrieve the local repository path using the followiwin search pattern:
     * <p/>
     * 1. System Property
     * 2. localRepository specified in user settings file
     * 3. ${user.home}/.m2/repository
     */
    public String getLocalRepositoryPath( Settings settings )
    {
        String localRepositoryPath = System.getProperty( ALT_LOCAL_REPOSITORY_LOCATION );

        if ( localRepositoryPath == null )
        {
            localRepositoryPath = settings.getLocalRepository();
        }

        if ( localRepositoryPath == null )
        {
            localRepositoryPath = defaultUserLocalRepository.getAbsolutePath();
        }

        return localRepositoryPath;
    }

    public String getLocalRepositoryPath()
        throws SettingsConfigurationException
    {
        return getLocalRepositoryPath( buildSettings( getUserSettingsPath( null ),
                                                      getGlobalSettingsPath(),
                                                      false,
                                                      true,
                                                      false,
                                                      false ) );
    }

    public ArtifactRepository getLocalRepository()
        throws SettingsConfigurationException
    {
        return createLocalRepository( new File( getLocalRepositoryPath() ) );
    }

    // ----------------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
