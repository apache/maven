package org.apache.maven.artifact.manager;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.AbstractArtifactComponent;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class DefaultWagonManager
    extends AbstractArtifactComponent
    implements WagonManager, Contextualizable
{
    private PlexusContainer container;

    public Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws ArtifactHandlerNotFoundException
    {
        Artifact artifact = new DefaultArtifact( groupId, artifactId, version, type );

        artifact.setPath( path( artifact ) );

        return artifact;
    }

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        Wagon wagon;

        try
        {
            wagon = (Wagon) container.lookup( Wagon.ROLE, protocol );
        }
        catch ( ComponentLookupException e )
        {
            throw new UnsupportedProtocolException( "Cannot find wagon which supports the requested protocol: " + protocol );
        }

        return wagon;
    }

    /**
     * @param wagon
     * @throws Exception
     * @todo how we can handle exception here? Maybe we should just swallow it?
     * Plexus exception handling is not very precise here (it sucks in short words:) )
     * Maybe plexus should not throw any exception here as this is internal problem of plexus
     * and any ingeration from outside or intelligent error handlig are rather excluded.
     */
    public void releaseWagon( Wagon wagon )
        throws Exception
    {
        container.release( wagon );
    }

    public void put( File source, Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        Wagon wagon = getWagon( repository.getProtocol() );

        wagon.connect( repository );

        wagon.put( source, path( artifact ) );

        wagon.disconnect();

        releaseWagon( wagon );
    }

    /**
     * Get the requested artifact from any of the remote repositories and place in
     * the specified local ArtifactRepository.
     *
     * @param artifact
     * @param remoteRepositories
     * @param localRepository
     * @throws TransferFailedException
     */
    public void get( Artifact artifact, Set remoteRepositories, ArtifactRepository localRepository )
        throws TransferFailedException
    {
        get( artifact, artifact.getFile(), remoteRepositories );
    }

    /**
     * Look in a set of repositories and return when the first valid artifact is
     * found.
     */

    /**
     * @param artifact
     * @param destination
     * @throws TransferFailedException
     * @todo I want to somehow plug artifact validators at such low level.
     * Simply if artifact was downloaded but it was rejected by validator(s)
     * the loop should continue. Some of the validators can be feeded directly using events
     * so number of i/o operation could be limited.
     * <p/>
     * If we won't plug validation process here the question is what we can do afterwards?
     * We don't know from which ArtifactRepository artifact was fetched and where we should restart.
     * We should be also fetching md5 sums and such from the same exact directory then artifacts
     * <p/>
     * @todo probably all exceptions should just be logged and continue
     * @todo is the exception for warnings logged at debug level correct?
     */
    public void get( Artifact artifact, File destination, Set repositories )
        throws TransferFailedException
    {
        boolean transfered = false;

        File temp = null;

        try
        {
            temp = new File( destination + ".tmp" );

            temp.deleteOnExit();
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( "Could not create temporary file for transfering artificat: " + artifact );
        }

        for ( Iterator iter = repositories.iterator(); iter.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) iter.next();

            try
            {
                Wagon wagon = getWagon( repository.getProtocol() );

                // ----------------------------------------------------------------------
                // These can certainly be configurable ... registering listeners ...

                //ChecksumObserver md5SumObserver = new ChecksumObserver();

                // ----------------------------------------------------------------------

                //wagon.addTransferListener( md5SumObserver );

                wagon.connect( repository );

                wagon.get( path( artifact ), temp );

                wagon.disconnect();

                releaseWagon( wagon );

            }
            catch ( ResourceDoesNotExistException e )
            {
                // This one we will eat when looking through remote repositories
                // because we want to cycle through them all before squawking.

                continue;
            }
            catch ( UnsupportedProtocolException e )
            {
                throw new TransferFailedException( "Unsupported Protocol: ", e );
            }
            catch ( ConnectionException e )
            {
                throw new TransferFailedException( "Connection failed: ", e );
            }
            catch ( AuthenticationException e )
            {
                throw new TransferFailedException( "Authentication failed: ", e );
            }
            catch ( AuthorizationException e )
            {
                throw new TransferFailedException( "Authorization failed: ", e );
            }
            catch ( TransferFailedException e )
            {
                getLogger().warn( "Failure getting artifact from repository '" + repository + "': " + e );
                getLogger().debug( "Stack trace", e );
                continue;
            }
            catch ( Exception e )
            {
                throw new TransferFailedException( "Release of wagon failed: ", e );
            }

            if ( !destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }

            // The temporary file is named destination + ".tmp" and is done this way to ensure
            // that the temporary file is in the same file system as the destination because the
            // File.renameTo operation doesn't really work across file systems. So we will attempt
            // to do a File.renameTo for efficiency and atomicity, if this fails then we will use
            // a brute force copy and delete the temporary file.

            if ( !temp.renameTo( destination ) )
            {
                try
                {
                    FileUtils.copyFile( temp, destination );

                    temp.delete();
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "Error copying temporary file to the final destination: ", e );
                }
            }

            return;
        }

        throw new TransferFailedException( "Unable to download the artifact from any repository" );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
