package org.apache.maven.artifact.transform;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 * @version $Id: SnapshotTransformation.java,v 1.1 2005/03/03 15:37:25
 *          jvanzyl Exp $
 */
public class SnapshotTransformation
    extends AbstractLogEnabled
    implements ArtifactTransformation
{
    private WagonManager wagonManager;

    /**
     * @todo very primitve. Probably we can cache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private static Set resolvedArtifactCache = new HashSet();

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata localMetadata;
            try
            {
                localMetadata = SnapshotArtifactMetadata.readFromLocalRepository( artifact, localRepository );
            }
            catch ( ArtifactPathFormatException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
            }

            boolean alreadyResolved = alreadyResolved( artifact );
            if ( !alreadyResolved )
            {
                boolean checkedUpdates = false;
                for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
                {
                    ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

                    String snapshotPolicy = remoteRepository.getSnapshotPolicy();
                    // TODO: should be able to calculate this less often
                    boolean checkForUpdates = false;
                    if ( ArtifactRepository.SNAPSHOT_POLICY_ALWAYS.equals( snapshotPolicy ) )
                    {
                        checkForUpdates = true;
                    }
                    else if ( ArtifactRepository.SNAPSHOT_POLICY_DAILY.equals( snapshotPolicy ) )
                    {
                        // Note that if last modified is 0, it didn't exist, so this will be true
                        if ( getMidnightBoundary().after( new Date( localMetadata.getLastModified() ) ) )
                        {
                            checkForUpdates = true;
                        }
                    }
                    else if ( snapshotPolicy.startsWith( ArtifactRepository.SNAPSHOT_POLICY_INTERVAL ) )
                    {
                        String s = snapshotPolicy.substring( ArtifactRepository.SNAPSHOT_POLICY_INTERVAL.length() + 1 );
                        int minutes = Integer.valueOf( s ).intValue();
                        Calendar cal = Calendar.getInstance();
                        cal.add( Calendar.MINUTE, -minutes );
                        // Note that if last modified is 0, it didn't exist, so this will be true
                        if ( cal.getTime().after( new Date( localMetadata.getLastModified() ) ) )
                        {
                            checkForUpdates = true;
                        }
                    }
                    // else assume "never"

                    if ( checkForUpdates )
                    {
                        getLogger().info(
                            artifact.getArtifactId() + ": checking for updates from " + remoteRepository.getId() );

                        SnapshotArtifactMetadata remoteMetadata = SnapshotArtifactMetadata.retrieveFromRemoteRepository(
                            artifact, remoteRepository, wagonManager );

                        if ( remoteMetadata.compareTo( localMetadata ) > 0 )
                        {
                            artifact.setRepository( remoteRepository );

                            localMetadata = remoteMetadata;
                        }
                        checkedUpdates = true;
                    }
                }

                if ( checkedUpdates )
                {
                    localMetadata.storeInLocalRepository( localRepository );
                }

                resolvedArtifactCache.add( getCacheKey( artifact ) );
            }

            // TODO: if the POM and JAR are inconsistent, this might mean that different version of each are used
            if ( artifact.getFile().exists() && !localMetadata.newerThanFile( artifact.getFile() ) )
            {
                if ( !alreadyResolved )
                {
                    // Locally installed file is newer, don't use the resolved version
                    getLogger().info( artifact.getArtifactId() + ": using locally installed snapshot" );
                }
            }
            else
            {
                String version = localMetadata.constructVersion();

                if ( getLogger().isInfoEnabled() )
                {
                    if ( !version.equals( artifact.getBaseVersion() ) && !alreadyResolved )
                    {
                        String message = artifact.getArtifactId() + ": resolved to version " + version;
                        if ( artifact.getRepository() != null )
                        {
                            message += " from repository " + artifact.getRepository().getId();
                        }
                        else
                        {
                            message += " from local repository";
                        }
                        getLogger().info( message );
                    }
                }

                artifact.setVersion( version );
                try
                {
                    artifact.setFile( new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) ) );
                }
                catch ( ArtifactPathFormatException e )
                {
                    throw new ArtifactMetadataRetrievalException( "Error reading local metadata", e );
                }
            }
        }
    }

    private Date getMidnightBoundary()
    {
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.HOUR_OF_DAY, 0 );
        cal.set( Calendar.MINUTE, 0 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        return cal.getTime();
    }

    private boolean alreadyResolved( Artifact artifact )
    {
        return resolvedArtifactCache.contains( getCacheKey( artifact ) );
    }

    private static String getCacheKey( Artifact artifact )
    {
        // No type - one per POM
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        try
        {
            SnapshotArtifactMetadata metadata = SnapshotArtifactMetadata.readFromLocalRepository( artifact,
                                                                                                  localRepository );
            if ( metadata.getLastModified() == 0 )
            {
                // doesn't exist - create to avoid an old snapshot download later
                metadata.storeInLocalRepository( localRepository );
            }
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error getting existing metadata", e );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( "Error getting existing metadata", e );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata metadata = SnapshotArtifactMetadata.retrieveFromRemoteRepository( artifact,
                                                                                                       remoteRepository,
                                                                                                       wagonManager );
            metadata.update();

            artifact.setVersion( metadata.constructVersion() );

            artifact.addMetadata( metadata );
        }
    }

    private static boolean isSnapshot( Artifact artifact )
    {
        return artifact.getVersion().endsWith( "SNAPSHOT" );
    }
}