import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class ArtifactDownloader
{
    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";
    
    private File mavenRepoLocal;
    
    private List remoteRepos;
    
    private boolean useTimestamp = true;

    private boolean ignoreErrors = true;
    
    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;
    
    public ArtifactDownloader( Properties properties ) throws Exception
    {
        setRemoteRepo( properties.getProperty( "maven.repo.remote" ) );

        String mavenRepoLocalProperty = properties.getProperty( "maven.repo.local" );

        if ( mavenRepoLocalProperty == null )
        {
            mavenRepoLocalProperty = System.getProperty( "user.home" ) + "/.maven/repository";
        }

        mavenRepoLocal = new File( mavenRepoLocalProperty );

        if ( !mavenRepoLocal.exists() )
        {
            if ( !mavenRepoLocal.mkdirs() )
            {
                System.err.println( "Cannot create the specified maven.repo.local: " + mavenRepoLocal );

                System.exit( 1 );
            }
        }

        if ( !mavenRepoLocal.canWrite() )
        {
            System.err.println( "Can't write to " + mavenRepoLocal.getAbsolutePath() );

            System.exit( 1 );
        }

        writeFile( "bootstrap.repo", mavenRepoLocal.getPath() );

        System.out.println( "Using the following for your maven.repo.local: " + mavenRepoLocal );
    }
    
    private void writeFile( String name, String contents )
        throws Exception
    {
        Writer writer = new FileWriter( name );

        writer.write( contents );

        writer.close();
    }

    public File getMavenRepoLocal()
    {
        return mavenRepoLocal;
    }
    
    public void downloadDependencies( List files )
        throws Exception
    {
        for ( Iterator j = files.iterator(); j.hasNext(); )
        {
            try
            {
                String file = (String) j.next();

                File destinationFile = new File( mavenRepoLocal, file );

                // The directory structure for this project may
                // not exists so create it if missing.
                File directory = destinationFile.getParentFile();

                if ( directory.exists() == false )
                {
                    directory.mkdirs();
                }

                if ( destinationFile.exists() && file.indexOf( SNAPSHOT_SIGNATURE ) < 0 )
                {
                    continue;
                }

                log( "Downloading dependency: " + file );

                getRemoteArtifact( file, destinationFile );

                if ( !destinationFile.exists() )
                {
                    throw new Exception( "Failed to download " + file );
                }
            }
            catch ( Exception e )
            {
                throw new Exception( e );
            }
        }
    }

    private void setRemoteRepo( String repos )
    {
        remoteRepos = new ArrayList();

        if ( repos == null )
        {
            remoteRepos.add( "http://www.ibiblio.org/maven/" );
            return;
        }
        
        StringTokenizer st = new StringTokenizer( repos, "," );
        while ( st.hasMoreTokens() )
        {
            remoteRepos.add( st.nextToken().trim() );
        }
    }

    private List getRemoteRepo()
    {
        return remoteRepos;
    }

    private boolean getRemoteArtifact( String file, File destinationFile )
    {
        boolean fileFound = false;

        for ( Iterator i = getRemoteRepo().iterator(); i.hasNext(); )
        {
            String remoteRepo = (String) i.next();

            // The username and password parameters are not being
            // used here. Those are the "" parameters you see below.
            String url = remoteRepo + "/" + file;

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

            // Attempt to retrieve the artifact and set the checksum if retrieval
            // of the checksum file was successful.
            try
            {
                HttpUtils.getFile( url,
                                   destinationFile,
                                   ignoreErrors,
                                   useTimestamp,
                                   proxyHost,
                                   proxyPort,
                                   proxyUserName,
                                   proxyPassword,
                                   true );

                // Artifact was found, continue checking additional remote repos (if any)
                // in case there is a newer version (i.e. snapshots) in another repo
                fileFound = true;
            }
            catch ( FileNotFoundException e )
            {
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
                log( "Error retrieving artifact from [" + url + "]: " );
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