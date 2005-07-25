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
import org.apache.maven.artifact.metadata.AbstractVersionArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.VersionArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Describes a version transformation during artifact resolution.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractVersionTransformation
    extends AbstractLogEnabled
    implements ArtifactTransformation
{
    protected WagonManager wagonManager;

    /**
     * @todo very primitve. Probably we can cache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private static Set resolvedArtifactCache = new HashSet();

    protected String resolveVersion( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        VersionArtifactMetadata localMetadata;
        try
        {
            localMetadata = readFromLocalRepository( artifact, localRepository );
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
                ArtifactRepository repository = (ArtifactRepository) i.next();

                ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots()
                    : repository.getReleases();

                if ( !policy.isEnabled() )
                {
                    getLogger().info( "Skipping disabled repository " + repository.getId() );
                }
                else
                {
                    String updatePolicy = policy.getUpdatePolicy();
                    // TODO: should be able to calculate this less often
                    boolean checkForUpdates = false;
                    if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( updatePolicy ) )
                    {
                        checkForUpdates = true;
                    }
                    else if ( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY.equals( updatePolicy ) )
                    {
                        if ( !localMetadata.checkedSinceDate( getMidnightBoundary() ) )
                        {
                            checkForUpdates = true;
                        }
                    }
                    else if ( updatePolicy.startsWith( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
                    {
                        String s = updatePolicy.substring(
                            ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL.length() + 1 );
                        int minutes = Integer.valueOf( s ).intValue();
                        Calendar cal = Calendar.getInstance();
                        cal.add( Calendar.MINUTE, -minutes );
                        if ( !localMetadata.checkedSinceDate( cal.getTime() ) )
                        {
                            checkForUpdates = true;
                        }
                    }
                    // else assume "never"

                    if ( checkForUpdates )
                    {
                        getLogger().info(
                            artifact.getArtifactId() + ": checking for updates from " + repository.getId() );

                        VersionArtifactMetadata remoteMetadata;

                        checkedUpdates = true;

                        try
                        {
                            remoteMetadata = retrieveFromRemoteRepository( artifact, repository, localMetadata,
                                                                           updatePolicy );
                        }
                        catch ( ResourceDoesNotExistException e )
                        {
                            getLogger().debug( "Error resolving artifact version from metadata.", e );

                            continue;
                        }

                        int difference = remoteMetadata.compareTo( localMetadata );
                        if ( difference > 0 )
                        {
                            // remote is newer
                            artifact.setRepository( repository );

                            localMetadata = remoteMetadata;
                        }
                    }
                }
            }

            if ( checkedUpdates )
            {
                localMetadata.storeInLocalRepository( localRepository );
            }

            resolvedArtifactCache.add( getCacheKey( artifact ) );
        }

        String version = localMetadata.constructVersion();

        // TODO: if the POM and JAR are inconsistent, this might mean that different version of each are used
        if ( !artifact.getFile().exists() || localMetadata.newerThanFile( artifact.getFile() ) )
        {
            if ( getLogger().isInfoEnabled() && !alreadyResolved )
            {
                if ( version != null && !version.equals( artifact.getBaseVersion() ) )
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

            return version;
        }
        else
        {
            if ( getLogger().isInfoEnabled() && !alreadyResolved )
            {
                // Locally installed file is newer, don't use the resolved version
                getLogger().info( artifact.getArtifactId() + ": using locally installed snapshot" );
            }
            return artifact.getVersion();
        }
    }

    protected VersionArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                    ArtifactRepository remoteRepository,
                                                                    VersionArtifactMetadata localMetadata,
                                                                    String updatePolicy )
        throws ArtifactMetadataRetrievalException, ResourceDoesNotExistException
    {
        AbstractVersionArtifactMetadata metadata = createMetadata( artifact );

        metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager, updatePolicy );

        return metadata;
    }

    protected abstract AbstractVersionArtifactMetadata createMetadata( Artifact artifact );

    private VersionArtifactMetadata readFromLocalRepository( Artifact artifact, ArtifactRepository localRepository )
        throws IOException
    {
        AbstractVersionArtifactMetadata metadata = createMetadata( artifact );
        metadata.readFromLocalRepository( localRepository );
        return metadata;
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
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
    }
}
