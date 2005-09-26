package org.apache.maven.embedder;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class MavenEmbedder
{
    // ----------------------------------------------------------------------
    // Embedder
    // ----------------------------------------------------------------------

    private Embedder embedder;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private MavenSettingsBuilder settingsBuilder;

    private MavenXpp3Reader modelReader;

    private ProfileManager profileManager;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------

    private Settings settings;

    private ArtifactRepository localRepository;

    private File localRepositoryDirectory;

    private ClassLoader classLoader;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------

    private boolean pluginUpdateOverride;

    private boolean checkLatestPluginVersion;

    private boolean interactiveMode;

    private boolean usePluginRegistry;

    private boolean offline;

    private boolean updateSnapshots;

    private String globalChecksumPolicy;

    public void setPluginUpdateOverride( boolean pluginUpdateOverride )
    {
        this.pluginUpdateOverride = pluginUpdateOverride;
    }

    public void setCheckLatestPluginVersion( boolean checkLatestPluginVersion )
    {
        this.checkLatestPluginVersion = checkLatestPluginVersion;
    }

    public void setInteractiveMode( boolean interactiveMode )
    {
        this.interactiveMode = interactiveMode;
    }

    public void setUsePluginRegistry( boolean usePluginRegistry )
    {
        this.usePluginRegistry = usePluginRegistry;
    }

    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    public void setUpdateSnapshots( boolean updateSnapshots )
    {
        this.updateSnapshots = updateSnapshots;
    }

    public void setGlobalChecksumPolicy( String globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    /**
     * Set the classloader to use with the maven embedder.
     *
     * @param classLoader
     */
    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public void setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }
    // ----------------------------------------------------------------------
    // Embedder Client Contract
    // ----------------------------------------------------------------------

    public Model readModel( File model )
        throws XmlPullParserException, FileNotFoundException, IOException
    {
        return modelReader.read( new FileReader( model ) );
    }

    public MavenProject readProject( File mavenProject )
        throws ProjectBuildingException
    {
        return mavenProjectBuilder.build( mavenProject, localRepository, profileManager );
    }

    public MavenProject readProjectWithDependencies( File mavenProject, TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager, transferListener );
    }

    public MavenProject readProjectWithDependencies( File mavenProject )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        return mavenProjectBuilder.buildWithDependencies( mavenProject, localRepository, profileManager );
    }

    // ----------------------------------------------------------------------
    // Internal utility code
    // ----------------------------------------------------------------------

    private ArtifactRepository createLocalRepository( Settings settings )
        throws ComponentLookupException
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );

        String url = settings.getLocalRepository();

        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", url, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            settings.setOffline( true );

            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy );

        return localRepository;
    }

    private RuntimeInfo createRuntimeInfo( Settings settings )
    {
        RuntimeInfo runtimeInfo = new RuntimeInfo( settings );

        if ( pluginUpdateOverride )
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.TRUE );
        }
        else
        {
            runtimeInfo.setPluginUpdateOverride( Boolean.FALSE );
        }

        if ( checkLatestPluginVersion )
        {
            runtimeInfo.setCheckLatestPluginVersion( Boolean.TRUE );
        }
        else
        {
            runtimeInfo.setCheckLatestPluginVersion( Boolean.FALSE );
        }

        return runtimeInfo;
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    public void start()
        throws MavenEmbedderException
    {
        if ( classLoader == null )
        {
            throw new IllegalStateException( "A classloader must be specified using setClassLoader(ClassLoader)." );
        }

        embedder = new Embedder();

        try
        {
            ClassWorld classWorld = new ClassWorld();

            classWorld.newRealm( "plexus.core", classLoader );

            embedder.start( classWorld );

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            profileManager = new DefaultProfileManager( embedder.getContainer() );

            mavenProjectBuilder = (MavenProjectBuilder) embedder.lookup( MavenProjectBuilder.ROLE );

            artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup( ArtifactRepositoryFactory.ROLE );

            // ----------------------------------------------------------------------
            // If an explicit local repository has not been set then we will use the
            // setting builder to use the maven defaults to help us find one.
            // ----------------------------------------------------------------------

            if ( localRepositoryDirectory == null )
            {
                settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );

                try
                {
                    settings = settingsBuilder.buildSettings();
                }
                catch ( IOException e )
                {
                    throw new MavenEmbedderException( "Error creating settings.", e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MavenEmbedderException( "Error creating settings.", e );
                }
            }
            else
            {
                settings = new Settings();

                settings.setLocalRepository( localRepositoryDirectory.getAbsolutePath() );
            }

            settings.setRuntimeInfo( createRuntimeInfo( settings ) );

            settings.setOffline( offline );

            settings.setUsePluginRegistry( usePluginRegistry );

            settings.setInteractiveMode( interactiveMode );

            localRepository = createLocalRepository( settings );
        }
        catch ( PlexusContainerException e )
        {
            throw new MavenEmbedderException( "Cannot start Plexus embedder.", e );
        }
        catch ( DuplicateRealmException e )
        {
            throw new MavenEmbedderException( "Cannot create Classworld realm for the embedder.", e );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

    public void stop()
        throws MavenEmbedderException
    {
        try
        {
            embedder.release( mavenProjectBuilder );

            embedder.release( artifactRepositoryFactory );

            embedder.release( settingsBuilder );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Cannot stop the embedder.", e );
        }
    }
}
