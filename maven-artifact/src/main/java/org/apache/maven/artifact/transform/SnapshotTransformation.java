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

/* TODO: use and remove
    public Artifact transform( Artifact artifact, ArtifactRepository localRepository, List repositories,
                               Map parameters )
        throws Exception
    {
        Date localVersion = getLocalVersion( artifact, localRepository );

        Date remoteVersion = getRemoteVersion( artifact, repositories, localRepository );

        if ( remoteVersion != null )
        {
            //if local version is unknown (null) it means that
            //we don't have this file locally. so we will be happy
            // to have any snapshot.
            // we wil download in two cases:
            //  a) we don't have any snapot in local repo
            //  b) we have found newer version in remote repository
            if ( localVersion == null || localVersion.before( remoteVersion ) )
            {
                // here we know that we have artifact like foo-1.2-SNAPSHOT.jar
                // and the remote timestamp is something like 20010304.121212
                // so we might as well fetch foo-1.2-20010304.121212.jar
                // but we are just going to fetch foo-1.2-SNAPSHOT.jar.
                // We can change the strategy which is used here later on

                // @todo we will delete old file first.
                //it is not really a right thing to do. Artifact Dowloader
                // should
                // fetch to temprary file and replace the old file with the new
                // one once download was finished

                artifact.getFile().delete();

                artifactResolver.resolve( artifact, repositories, localRepository );

                File snapshotVersionFile = getSnapshotVersionFile( artifact, localRepository );

                String timestamp = getTimestamp( remoteVersion );

                // delete old one
                if ( snapshotVersionFile.exists() )
                {
                    snapshotVersionFile.delete();
                }

                FileUtils.fileWrite( snapshotVersionFile.getPath(), timestamp );
            }
        }

        return artifact;
    }

    private File getSnapshotVersionFile( Artifact artifact, ArtifactRepository localRepository )
    {
        return null;
        //return new File( localRepository.fullArtifactPath( artifact ) );
    }

    private Date getRemoteVersion( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws Exception
    {
        Date retValue = null;

        artifactResolver.resolve( artifact, remoteRepositories, localRepository );

        String timestamp = FileUtils.fileRead( artifact.getPath() );

        retValue = parseTimestamp( timestamp );

        return retValue;
    }

    private Date getLocalVersion( Artifact artifact, ArtifactRepository localRepository )
    {
        //assert artifact.exists();

        Date retValue = null;

        try
        {
            File file = getSnapshotVersionFile( artifact, localRepository );

            if ( file.exists() )
            {
                String timestamp = FileUtils.fileRead( file );

                retValue = parseTimestamp( timestamp );

            }
        }
        catch ( Exception e )
        {
            // ignore
        }

        if ( retValue == null )
        {
            //try "traditional method" used in maven1 for obtaining snapshot
            // version

            File file = artifact.getFile();

            if ( file.exists() )
            {
                retValue = new Date( file.lastModified() );

                //@todo we should "normalize" the time.

                //
                // TimeZone gmtTimeZone = TimeZone.getTimeZone( "GMT" );
                // TimeZone userTimeZone = TimeZone.getDefault(); long diff =
                //
            }
        }

        return retValue;
    }

    private final static String DATE_FORMAT = "yyyyMMdd.HHmmss";

    private static SimpleDateFormat getFormatter()
    {
        SimpleDateFormat formatter = new SimpleDateFormat( DATE_FORMAT );

        formatter.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        return formatter;
    }

    public static String getTimestamp()
    {
        Date now = new Date();

        SimpleDateFormat formatter = getFormatter();

        String retValue = formatter.format( now );

        return retValue;
    }

    public static Date parseTimestamp( String timestamp )
        throws ParseException
    {
        Date retValue = getFormatter().parse( timestamp );

        return retValue;
    }

    public static String getTimestamp( Date snapshotVersion )
    {
        String retValue = getFormatter().format( snapshotVersion );

        return retValue;
    }
    */
    public Artifact transformForResolve( Artifact artifact )
    {
        // TODO: implement
        return artifact;
    }

    public Artifact transformForInstall( Artifact artifact, ArtifactRepository localRepository )
    {
        if ( isSnapshot( artifact ) )
        {
            // only store the version-local.txt file for POMs as every file has an associated POM
            ArtifactMetadata metadata = SnapshotArtifactMetadata.createLocalSnapshotMetadata( artifact );
            artifact.addMetadata( metadata );
        }
        return artifact;
    }

    public Artifact transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata metadata = SnapshotArtifactMetadata.createRemoteSnapshotMetadata( artifact );
            metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager );
            metadata.update();

            // TODO: note, we could currently transform this in place, as it is only used through the deploy mojo,
            //   which creates the artifact and then disposes of it
            artifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), metadata.getVersion(),
                                            artifact.getScope(), artifact.getType(), artifact.getClassifier() );
            artifact.addMetadata( metadata );
        }
        return artifact;
    }

    private static boolean isSnapshot( Artifact artifact )
    {
        return artifact.getVersion().endsWith( "SNAPSHOT" );
    }
}