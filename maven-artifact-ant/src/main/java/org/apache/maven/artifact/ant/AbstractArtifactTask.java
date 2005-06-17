package org.apache.maven.artifact.ant;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.profiles.activation.ProfileActivationUtils;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Base class for artifact tasks.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractArtifactTask
    extends Task
{
    private Settings settings;

    private Embedder embedder;

    private Pom pom;

    private String pomRefId;

    private LocalRepository localRepository;

    protected ArtifactRepository createLocalArtifactRepository()
    {
        if ( localRepository == null )
        {
            localRepository = getDefaultLocalRepository();
        }

        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       localRepository.getLayout() );

        CustomWagonManager manager = (CustomWagonManager) lookup( WagonManager.ROLE );
        manager.setLocalRepository( localRepository.getLocation() );

        return new ArtifactRepository( "local", "file://" + localRepository.getLocation(), repositoryLayout );
    }

    protected ArtifactRepository createRemoteArtifactRepository( RemoteRepository repository )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       repository.getLayout() );

        WagonManager manager = (WagonManager) lookup( WagonManager.ROLE );

        Authentication authentication = repository.getAuthentication();
        if ( authentication != null )
        {
            manager.addAuthenticationInfo( "remote", authentication.getUserName(), authentication.getPassword(),
                                           authentication.getPrivateKey(), authentication.getPassphrase() );
        }

        Proxy proxy = repository.getProxy();
        if ( proxy != null )
        {
            manager.addProxy( proxy.getType(), proxy.getHost(), proxy.getPort(), proxy.getUserName(),
                              proxy.getPassword(), proxy.getNonProxyHosts() );
        }

        ArtifactRepository artifactRepository;
        if ( repository.getSnapshotPolicy() != null )
        {
            artifactRepository = new ArtifactRepository( "remote", repository.getUrl(), repositoryLayout,
                                                         repository.getSnapshotPolicy() );
        }
        else
        {
            artifactRepository = new ArtifactRepository( "remote", repository.getUrl(), repositoryLayout );
        }
        return artifactRepository;
    }

    protected LocalRepository getDefaultLocalRepository()
    {
        Settings settings = getSettings();
        LocalRepository localRepository = new LocalRepository();
        localRepository.setLocation( new File( settings.getLocalRepository() ) );
        return localRepository;
    }

    protected synchronized Settings getSettings()
    {
        if ( settings == null )
        {
            settings = new Settings();

            File settingsFile = new File( System.getProperty( "user.home" ), ".ant/settings.xml" );
            if ( !settingsFile.exists() )
            {
                settingsFile = new File( System.getProperty( "user.home" ), ".m2/settings.xml" );
            }

            if ( settingsFile.exists() )
            {
                FileReader reader = null;
                try
                {
                    reader = new FileReader( settingsFile );

                    SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

                    settings = modelReader.read( reader );
                }
                catch ( IOException e )
                {
                    log( "Error reading settings file '" + settingsFile + "' - ignoring. Error was: " + e.getMessage(),
                         Project.MSG_WARN );
                }
                catch ( XmlPullParserException e )
                {
                    log( "Error parsing settings file '" + settingsFile + "' - ignoring. Error was: " + e.getMessage(),
                         Project.MSG_WARN );
                }
                finally
                {
                    IOUtil.close( reader );
                }
            }

            if ( StringUtils.isEmpty( settings.getLocalRepository() ) )
            {
                String location = new File( System.getProperty( "user.home" ), ".m2/repository" ).getAbsolutePath();
                settings.setLocalRepository( location );
            }
        }
        return settings;
    }

    protected RemoteRepository createAntRemoteRepository( org.apache.maven.model.Repository pomRepository )
    {
        // TODO: actually, we need to not funnel this through the ant repository - we should pump settings into wagon
        // manager at the start like m2 does, and then match up by repository id
        // As is, this could potentially cause a problem with 2 remote repositories with different authentication info  

        RemoteRepository r = new RemoteRepository();
        r.setUrl( pomRepository.getUrl() );
        r.setSnapshotPolicy( pomRepository.getSnapshotPolicy() );
        r.setLayout( pomRepository.getLayout() );

        Server server = getSettings().getServer( pomRepository.getId() );
        if ( server != null )
        {
            r.addAuthentication( new Authentication( server ) );
        }

        org.apache.maven.settings.Proxy proxy = getSettings().getActiveProxy();
        if ( proxy != null )
        {
            r.addProxy( new Proxy( proxy ) );
        }

        Mirror mirror = getSettings().getMirrorOf( pomRepository.getId() );
        if ( mirror != null )
        {
            r.setUrl( mirror.getUrl() );
        }

        return r;
    }

    protected Object lookup( String role )
    {
        try
        {
            return getEmbedder().lookup( role );
        }
        catch ( ComponentLookupException e )
        {
            throw new BuildException( "Unable to find component: " + role, e );
        }
    }

    protected Object lookup( String role, String roleHint )
    {
        try
        {
            return getEmbedder().lookup( role, roleHint );
        }
        catch ( ComponentLookupException e )
        {
            throw new BuildException( "Unable to find component: " + role + "[" + roleHint + "]", e );
        }
    }

    private synchronized Embedder getEmbedder()
    {
        if ( embedder == null )
        {
            embedder = (Embedder) getProject().getReference( Embedder.class.getName() );

            if ( embedder == null )
            {
                embedder = new Embedder();
                try
                {
                    embedder.start();
                }
                catch ( PlexusContainerException e )
                {
                    throw new BuildException( "Unable to start embedder", e );
                }
                getProject().addReference( Embedder.class.getName(), embedder );
            }
        }
        return embedder;
    }

    public Pom buildPom( MavenProjectBuilder projectBuilder, ArtifactRepository localArtifactRepository )
    {
        if ( pomRefId != null && pom != null )
        {
            throw new BuildException( "You cannot specify both a POM element and a pomrefid element" );
        }

        Pom pom = this.pom;
        if ( pomRefId != null )
        {
            pom = (Pom) getProject().getReference( pomRefId );
            if ( pom == null )
            {
                throw new BuildException( "Reference '" + pomRefId + "' was not found." );
            }
        }

        if ( pom != null )
        {
            pom.initialise( projectBuilder, localArtifactRepository );
        }
        return pom;
    }

    public void addPom( Pom pom )
    {
        this.pom = pom;
    }

    public String getPomRefId()
    {
        return pomRefId;
    }

    public void setPomRefId( String pomRefId )
    {
        this.pomRefId = pomRefId;
    }

    public LocalRepository getLocalRepository()
    {
        return localRepository;
    }

    public void addLocalRepository( LocalRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public void setProfiles( String profiles )
    {
        if ( profiles != null )
        {
            // TODO: not sure this is the best way to do this...
            System.setProperty( ProfileActivationUtils.ACTIVE_PROFILE_IDS, profiles );
        }
    }

    protected Artifact createArtifact( Pom pom )
    {
        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        Artifact artifact = factory.createArtifact( pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), null,
                                                    pom.getPackaging() );
        return artifact;
    }
}
