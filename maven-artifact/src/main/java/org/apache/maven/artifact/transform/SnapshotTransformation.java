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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;

import java.io.IOException;
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
    implements ArtifactTransformation
{
    private WagonManager wagonManager;

    /**
     * @todo very primitve. Probably we can resolvedArtifactCache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private static Set resolvedArtifactCache = new HashSet();

    public Artifact transformForResolve( Artifact artifact, List remoteRepositories,
                                         ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        // TODO: remove hack
        if ( isSnapshot( artifact ) &&
            !Boolean.valueOf( System.getProperty( "maven.debug.snapshot.disabled", "true" ) ).booleanValue() )
        {
            // TODO: this mostly works, however...
            //  - poms and jars are different, so both are checked individually
            //  - when a pom is downloaded, it prevents the JAR getting downloaded because of the timestamp
            //  - need to gather first, group them all up by groupId/artifactId, then go after them
            //  - alternatively, keep the timestamp when downloading (as is done here), and use the SNAPSHOT file for install
            //  - however, there is no mechanism to flip back and forward, and presently it keeps looking for 2.0-TIMESTAMP-0 instead as that is in the build file
            //  - we definitely need the manual/daily check as this is quite slow given the large number of snapshots inside m2 presently

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

            if ( !alreadyResolved( artifact ) )
            {
                boolean foundRemote = false;
                for ( Iterator i = remoteRepositories.iterator(); i.hasNext(); )
                {
                    ArtifactRepository remoteRepository = (ArtifactRepository) i.next();

                    SnapshotArtifactMetadata remoteMetadata = SnapshotArtifactMetadata.retrieveFromRemoteRepository(
                        artifact, remoteRepository, wagonManager );

                    if ( remoteMetadata.compareTo( localMetadata ) > 0 )
                    {
                        // TODO: investigate transforming this in place, in which case resolve can return void
                        artifact.setRepository( remoteRepository );

                        localMetadata = remoteMetadata;
                        foundRemote = true;
                    }
                }

                if ( foundRemote )
                {
                    artifact.addMetadata( localMetadata );
                }
            }

            artifact = createArtifactCopy( artifact, localMetadata );

            resolvedArtifactCache.add( getCacheKey( artifact ) );
        }
        return artifact;
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

    public Artifact transformForInstall( Artifact artifact, ArtifactRepository localRepository )
    {
        // Nothing to do
/* TODO: remove
        if ( isSnapshot( artifact ) )
        {
            // only store the version-local.txt file for POMs as every file has an associated POM
            ArtifactMetadata metadata = SnapshotArtifactMetadata.createLocalSnapshotMetadata( artifact );
            artifact.addMetadata( metadata );
        }
*/
        return artifact;
    }

    public Artifact transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata metadata = SnapshotArtifactMetadata.retrieveFromRemoteRepository( artifact,
                                                                                                       remoteRepository,
                                                                                                       wagonManager );
            metadata.update();

            // TODO: note, we could currently transform this in place, as it is only used through the deploy mojo,
            //   which creates the artifact and then disposes of it
            artifact = createArtifactCopy( artifact, metadata );
            artifact.addMetadata( metadata );
        }
        return artifact;
    }

    private Artifact createArtifactCopy( Artifact artifact, SnapshotArtifactMetadata metadata )
    {
        Artifact newArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                    metadata.constructVersion(), artifact.getScope(),
                                                    artifact.getType(), artifact.getClassifier() );
        newArtifact.setBaseVersion( artifact.getBaseVersion() );

        for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
        {
            ArtifactMetadata m = (ArtifactMetadata) i.next();
            m.setArtifact( newArtifact );
            newArtifact.addMetadata( m );
        }

        newArtifact.setRepository( artifact.getRepository() );

        return newArtifact;
    }

    private static boolean isSnapshot( Artifact artifact )
    {
        return artifact.getVersion().endsWith( "SNAPSHOT" );
    }
}