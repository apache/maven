package org.apache.maven.artifact.resolver.transform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka </a>
 * @version $Id: SnapshotRequestTransformation.java,v 1.1 2005/03/03 15:37:25
 *          jvanzyl Exp $
 */
public class SnapshotRequestTransformation
    implements ArtifactRequestTransformation
{
    private ArtifactResolver artifactResolver;

    public Artifact transform( Artifact artifact, ArtifactRepository localRepository, List repositories, Map parameters )
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

                /*
                 * TimeZone gmtTimeZone = TimeZone.getTimeZone( "GMT" );
                 * TimeZone userTimeZone = TimeZone.getDefault(); long diff =
                 */
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

    public static Date parseTimestamp( String timestamp ) throws ParseException
    {
        Date retValue = getFormatter().parse( timestamp );

        return retValue;
    }

    public static String getTimestamp( Date snapshotVersion )
    {
        String retValue = getFormatter().format( snapshotVersion );

        return retValue;
    }
}