package download;

import model.Dependency;
import model.Repository;
import util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ArtifactDownloader
{
    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private boolean useTimestamp = true;

    private boolean ignoreErrors = false;

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private Repository localRepository;

    private static final String REPO_URL = "http://repo1.maven.org/maven2";

    private Map downloadedArtifacts = new HashMap();

    private List remoteRepositories;

    public ArtifactDownloader( Repository localRepository )
        throws Exception
    {
        if ( localRepository == null )
        {
            System.err.println( "local repository not specified" );

            System.exit( 1 );
        }

        this.localRepository = localRepository;
    }

    public void setProxy( String host, String port, String userName, String password )
    {
        proxyHost = host;
        proxyPort = port;
        proxyUserName = userName;
        proxyPassword = password;
        System.out.println( "Using the following proxy : " + proxyHost + "/" + proxyPort );
    }

    public void downloadDependencies( Collection dependencies )
        throws DownloadFailedException
    {
        for ( Iterator j = dependencies.iterator(); j.hasNext(); )
        {
            Dependency dep = (Dependency) j.next();

            String dependencyConflictId = dep.getDependencyConflictId();
            if ( !downloadedArtifacts.containsKey( dependencyConflictId ) )
            {
                File destinationFile = localRepository.getArtifactFile( dep );
                // The directory structure for this project may
                // not exists so create it if missing.
                File directory = destinationFile.getParentFile();

                if ( directory.exists() == false )
                {
                    directory.mkdirs();
                }

                boolean snapshot = isSnapshot( dep );

                if ( dep.getGroupId().equals( "org.apache.maven" ) && snapshot )
                {
                    //skip our own
                    continue;
                }

                if ( !getRemoteArtifact( dep, destinationFile ) )
                {
                    throw new DownloadFailedException( "Failed to download " + dep );
                }

                downloadedArtifacts.put( dependencyConflictId, dep );
            }
            else
            {
                Dependency d = (Dependency) downloadedArtifacts.get( dependencyConflictId );
                dep.setResolvedVersion( d.getResolvedVersion() );
            }
        }
    }

    private static boolean isSnapshot( Dependency dep )
    {
        return dep.getVersion().indexOf( SNAPSHOT_SIGNATURE ) >= 0;
    }

    private boolean getRemoteArtifact( Dependency dep, File destinationFile )
    {
        boolean fileFound = false;

        for ( Iterator i = getRemoteRepositories().iterator(); i.hasNext(); )
        {
            Repository remoteRepo = (Repository) i.next();

            // The username and password parameters are not being used here.
            String url = remoteRepo.getBasedir() + "/" + remoteRepo.getArtifactPath( dep );

            // Attempt to retrieve the artifact and set the checksum if retrieval
            // of the checksum file was successful.
            try
            {
                String version = dep.getVersion();
                if ( isSnapshot( dep ) )
                {
                    String filename = getSnapshotMetadataFile( destinationFile.getName(), "SNAPSHOT.version.txt" );
                    File file = localRepository.getMetadataFile( dep.getGroupId(), dep.getArtifactId(),
                                                                 dep.getVersion(), dep.getType(), filename );
                    String metadataPath = remoteRepo.getMetadataPath( dep.getGroupId(), dep.getArtifactId(),
                                                                      dep.getVersion(), dep.getType(), filename );
                    String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                    log( "Downloading " + metaUrl );
                    try
                    {
                        HttpUtils.getFile( metaUrl, file, ignoreErrors, false, proxyHost, proxyPort, proxyUserName,
                                           proxyPassword, false );
                        version = FileUtils.fileRead( file );
                        log( "Resolved version: " + version );
                        dep.setResolvedVersion( version );
                        String ver = version.substring( version.lastIndexOf( "-", version.lastIndexOf( "-" ) - 1 ) + 1 );
                        String extension = url.substring( url.length() - 4 );
                        url = getSnapshotMetadataFile( url, ver + extension );
                    }
                    catch ( IOException e )
                    {
                        log( "WARNING: SNAPSHOT version not found, using default: " + e.getMessage() );
                    }
                }
                if ( !dep.getType().equals( "pom" ) )
                {
                    File file = localRepository.getMetadataFile( dep.getGroupId(), dep.getArtifactId(),
                                                                 dep.getVersion(), dep.getType(),
                                                                 dep.getArtifactId() + "-" + dep.getResolvedVersion() +
                                                                 ".pom" );

                    file.getParentFile().mkdirs();

                    if ( !file.exists() || version.indexOf( "SNAPSHOT" ) >= 0 )
                    {
                        String filename = dep.getArtifactId() + "-" + version + ".pom";
                        String metadataPath = remoteRepo.getMetadataPath( dep.getGroupId(), dep.getArtifactId(),
                                                                          dep.getVersion(), dep.getType(), filename );
                        String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                        log( "Downloading " + metaUrl );

                        try
                        {
                            HttpUtils.getFile( metaUrl, file, ignoreErrors, false, proxyHost, proxyPort, proxyUserName,
                                               proxyPassword, false );
                        }
                        catch ( IOException e )
                        {
                            log( "Couldn't find POM - ignoring: " + e.getMessage() );
                        }
                    }
                }

                destinationFile = localRepository.getArtifactFile( dep );
                if ( !destinationFile.exists() || version.indexOf( "SNAPSHOT" ) >= 0 )
                {
                    log( "Downloading " + url );
                    HttpUtils.getFile( url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort,
                                       proxyUserName, proxyPassword, true );
                }

                // Artifact was found, continue checking additional remote repos (if any)
                // in case there is a newer version (i.e. snapshots) in another repo
                fileFound = true;
            }
            catch ( FileNotFoundException e )
            {
                log( "Artifact not found at [" + url + "]" );
                // Ignore
            }
            catch ( Exception e )
            {
                // If there are additional remote repos, then ignore exception
                // as artifact may be found in another remote repo. If there
                // are no more remote repos to check and the artifact wasn't found in
                // a previous remote repo, then artifactFound is false indicating
                // that the artifact could not be found in any of the remote repos
                //
                // arguably, we need to give the user better control (another command-
                // line switch perhaps) of what to do in this case? Maven already has
                // a command-line switch to work in offline mode, but what about when
                // one of two or more remote repos is unavailable? There may be multiple
                // remote repos for redundancy, in which case you probably want the build
                // to continue. There may however be multiple remote repos because some
                // artifacts are on one, and some are on another. In this case, you may
                // want the build to break.
                //
                // print a warning, in any case, so user catches on to mistyped
                // hostnames, or other snafus
                log( "Error retrieving artifact from [" + url + "]: " + e );
            }
        }

        return fileFound;
    }

    private static String getSnapshotMetadataFile( String filename, String s )
    {
        int index = filename.lastIndexOf( "SNAPSHOT" );
        return filename.substring( 0, index ) + s;
    }

    private void log( String message )
    {
        System.out.println( message );
    }

    public Repository getLocalRepository()
    {
        return localRepository;
    }

    public List getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList();
        }

        if ( remoteRepositories.isEmpty() )
        {
            // TODO: use super POM?
            remoteRepositories.add( new Repository( "central", REPO_URL, Repository.LAYOUT_DEFAULT ) );
        }

        return remoteRepositories;
    }

    public void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }
}
