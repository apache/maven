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
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
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
    private Embedder embedder;

    private Settings settings;

    protected ArtifactRepository createLocalArtifactRepository( LocalRepository repository )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       repository.getLayout() );

        CustomWagonManager manager = (CustomWagonManager) lookup( WagonManager.ROLE );
        manager.setLocalRepository( repository.getLocation() );

        return new ArtifactRepository( "local", "file://" + repository.getLocation(), repositoryLayout );
    }

    protected ArtifactRepository createRemoteArtifactRepository( RemoteRepository repository )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                       repository.getLayout() );

        Authentication authentication = repository.getAuthentication();
        if ( authentication != null )
        {
            WagonManager manager = (WagonManager) lookup( WagonManager.ROLE );
            manager.addAuthenticationInfo( "remote", authentication.getUserName(), authentication.getPassword(),
                                           authentication.getPrivateKey(), authentication.getPassphrase() );
        }

        return new ArtifactRepository( "remote", repository.getUrl(), repositoryLayout );
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

    private Object lookup( String role, String roleHint )
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
}
