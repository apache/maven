import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtils
{
    public static void useProxyUser( final String proxyHost,
                                     final String proxyPort,
                                     final String proxyUserName,
                                     final String proxyPassword )
    {
        if ( proxyHost != null && proxyPort != null )
        {
            System.getProperties().put( "proxySet", "true" );
            System.getProperties().put( "proxyHost", proxyHost );
            System.getProperties().put( "proxyPort", proxyPort );

            if ( proxyUserName != null )
            {
                Authenticator.setDefault( new Authenticator()
                {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication( proxyUserName,
                                                           proxyPassword == null ? new char[0] : proxyPassword.toCharArray() );
                    }
                } );
            }
        }
    }

    public static void getFile( String url,
                                File destinationFile,
                                boolean ignoreErrors,
                                boolean useTimestamp,
                                String proxyHost,
                                String proxyPort,
                                String proxyUserName,
                                String proxyPassword,
                                boolean useChecksum )
        throws Exception
    {
        // Get the requested file.
        getFile( url,
                 destinationFile,
                 ignoreErrors,
                 useTimestamp,
                 proxyHost,
                 proxyPort,
                 proxyUserName,
                 proxyPassword );

        // Get the checksum if requested.
        if ( useChecksum )
        {
            File checksumFile = new File( destinationFile + ".md5" );

            try
            {
                getFile( url + ".md5",
                         checksumFile,
                         ignoreErrors,
                         useTimestamp,
                         proxyHost,
                         proxyPort,
                         proxyUserName,
                         proxyPassword );
            }
            catch ( Exception e )
            {
                // do nothing we will check later in the process
                // for the checksums.
            }
        }
    }

    public static void getFile( String url,
                                File destinationFile,
                                boolean ignoreErrors,
                                boolean useTimestamp,
                                String proxyHost,
                                String proxyPort,
                                String proxyUserName,
                                String proxyPassword )
        throws Exception
    {
        String[] s = parseUrl( url );
        String username = s[0];
        String password = s[1];
        String parsedUrl = s[2];

        URL source = new URL( parsedUrl );

        //set the timestamp to the file date.
        long timestamp = 0;
        boolean hasTimestamp = false;
        if ( useTimestamp && destinationFile.exists() )
        {
            timestamp = destinationFile.lastModified();
            hasTimestamp = true;
        }

        //set proxy connection
        useProxyUser( proxyHost, proxyPort, proxyUserName, proxyPassword );

        //set up the URL connection
        URLConnection connection = source.openConnection();
        //modify the headers
        //NB: things like user authentication could go in here too.
        if ( useTimestamp && hasTimestamp )
        {
            connection.setIfModifiedSince( timestamp );
        }
        // prepare Java 1.1 style credentials
        if ( username != null || password != null )
        {
            String up = username + ":" + password;
            String encoding = null;
            // check to see if sun's Base64 encoder is available.
            try
            {
                sun.misc.BASE64Encoder encoder =
                    (sun.misc.BASE64Encoder) Class.forName(
                        "sun.misc.BASE64Encoder" ).newInstance();

                encoding = encoder.encode( up.getBytes() );
            }
            catch ( Exception ex )
            {
                // Do nothing, as for MavenSession we will never use
                // auth and we will eventually move over httpclient
                // in the commons.
            }
            connection.setRequestProperty( "Authorization", "Basic " + encoding );
        }

        //connect to the remote site (may take some time)
        connection.connect();
        //next test for a 304 result (HTTP only)
        if ( connection instanceof HttpURLConnection )
        {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED )
            {
                return;
            }
            // test for 401 result (HTTP only)
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED )
            {
                throw new Exception( "Not authorized." );
            }
        }

        // REVISIT: at this point even non HTTP connections may support the
        // if-modified-since behaviour - we just check the date of the
        // content and skip the write if it is not newer.
        // Some protocols (FTP) dont include dates, of course.

        InputStream is = null;
        for ( int i = 0; i < 3; i++ )
        {
            try
            {
                is = connection.getInputStream();
                break;
            }
            catch ( IOException ex )
            {
                // do nothing
            }
        }
        if ( is == null )
        {
            if ( ignoreErrors )
            {
                return;
            }

            // This will never happen with maven's use of this class.
            throw new Exception( "Can't get " + destinationFile.getName() + " to " + destinationFile );
        }

        FileOutputStream fos = new FileOutputStream( destinationFile );

        byte[] buffer = new byte[100 * 1024];
        int length;

        while ( ( length = is.read( buffer ) ) >= 0 )
        {
            fos.write( buffer, 0, length );
            System.out.print( "." );
        }

        System.out.println();
        fos.close();
        is.close();

        // if (and only if) the use file time option is set, then the
        // saved file now has its timestamp set to that of the downloaded
        // file
        if ( useTimestamp )
        {
            long remoteTimestamp = connection.getLastModified();

            if ( remoteTimestamp != 0 )
            {
                touchFile( destinationFile, remoteTimestamp );
            }
        }
    }

    static String[] parseUrl( String url )
    {
        String[] parsedUrl = new String[3];
        parsedUrl[0] = null;
        parsedUrl[1] = null;
        parsedUrl[2] = url;

        // We want to be able to deal with Basic Auth where the username
        // and password are part of the URL. An example of the URL string
        // we would like to be able to parse is like the following:
        //
        // http://username:password@repository.mycompany.com

        int i = url.indexOf( "@" );
        if ( i > 0 )
        {
            String s = url.substring( 7, i );
            int j = s.indexOf( ":" );
            parsedUrl[0] = s.substring( 0, j );
            parsedUrl[1] = s.substring( j + 1 );
            parsedUrl[2] = "http://" + url.substring( i + 1 );
        }

        return parsedUrl;
    }

    private static boolean touchFile( File file, long timemillis )
        throws Exception
    {
        long modifiedTime;

        if ( timemillis < 0 )
        {
            modifiedTime = System.currentTimeMillis();
        }
        else
        {
            modifiedTime = timemillis;
        }

        file.setLastModified( modifiedTime );

        return true;
    }
}
