package download;

import model.Dependency;
import model.Repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ArtifactDownloader
{
    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private List remoteRepos;

    private boolean useTimestamp = true;

    private boolean ignoreErrors = false;

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private Repository localRepository;

    public ArtifactDownloader( Repository localRepository, List remoteRepositories )
        throws Exception
    {
        setRemoteRepos( remoteRepositories );

        if ( localRepository == null )
        {
            System.err.println( "local repository not specified" );

            System.exit( 1 );
        }

        this.localRepository = localRepository;

        System.out.println( "Using the following for your local repository: " + localRepository );
        System.out.println( "Using the following for your remote repositories: " + remoteRepos );
    }

    private Set downloadedArtifacts = new HashSet();

    public void setProxy( String host, String port, String userName, String password )
    {
        proxyHost = host;
        proxyPort = port;
        proxyUserName = userName;
        proxyPassword = password;
        System.out.println( "Using the following proxy : " + proxyHost + "/" + proxyPort );
    }

    public void downloadDependencies( List files )
        throws Exception
    {
        for ( Iterator j = files.iterator(); j.hasNext(); )
        {
            Dependency dep = (Dependency) j.next();

            if ( !downloadedArtifacts.contains( dep.getId() ) )
            {
                File destinationFile = localRepository.getArtifactFile( dep );
                // The directory structure for this project may
                // not exists so create it if missing.
                File directory = destinationFile.getParentFile();

                if ( directory.exists() == false )
                {
                    directory.mkdirs();
                }

                if ( destinationFile.exists() && dep.getVersion().indexOf( SNAPSHOT_SIGNATURE ) < 0 )
                {
                    continue;
                }

                getRemoteArtifact( dep, destinationFile );

                if ( !destinationFile.exists() )
                {
                    throw new Exception( "Failed to download " + dep );
                }

                downloadedArtifacts.add( dep.getId() );
            }
        }
    }

    private void setRemoteRepos( List repositories )
    {
        remoteRepos = new ArrayList();

        if ( repositories != null )
        {
            remoteRepos.addAll( repositories );
        }

        if ( repositories.isEmpty() )
        {
            // TODO: use super POM?
            Repository repository = new Repository();
            repository.setBasedir( "http://repo1.maven.org" );
            remoteRepos.add( repository );
        }
    }

    private boolean getRemoteArtifact( Dependency dep, File destinationFile )
    {
        boolean fileFound = false;

        for ( Iterator i = remoteRepos.iterator(); i.hasNext(); )
        {
            Repository remoteRepo = (Repository) i.next();

            // The username and password parameters are not being
            // used here. Those are the "" parameters you see below.
            String url = remoteRepo.getBasedir() + "/" + remoteRepo.getArtifactPath( dep );

            if ( !url.startsWith( "file" ) )
            {
                url = replace( url, "//", "/" );
                if ( url.startsWith( "https" ) )
                {
                    url = replace( url, "https:/", "https://" );
                }
                else
                {
                    url = replace( url, "http:/", "http://" );
                }
            }
            else
            {
                // THe JDK URL for file: should have one or no / instead of // for some reason
                url = replace( url, "file://", "file:" );
            }

            // Attempt to retrieve the artifact and set the checksum if retrieval
            // of the checksum file was successful.
            try
            {
                log( "Downloading " + url );
                HttpUtils.getFile( url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort,
                                   proxyUserName, proxyPassword, true );

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

    private String replace( String text, String repl, String with )
    {
        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }

    private void log( String message )
    {
        System.out.println( message );
    }
}
