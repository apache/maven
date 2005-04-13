package download;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

/**
 * Http utils for retrieving files.
 *
 * @author costin@dnt.ro
 * @author gg@grtmail.com (Added Java 1.1 style HTTP basic auth)
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @todo Need to add a timeout so we can flip to a backup repository.
 * @todo Download everything in a single session.
 * @todo Throw meaningful exception when authentication fails.
 */
public class HttpUtils
{
    /**
     * Use a proxy to bypass the firewall with or without authentication
     *
     * @param proxyHost     Proxy Host (if proxy is required), or null
     * @param proxyPort     Proxy Port (if proxy is required), or null
     * @param proxyUserName Proxy Username (if authentification is required),
     *                      or null
     * @param proxyPassword Proxy Password (if authentification is required),
     *                      or null
     * @throws SecurityException if an operation is not authorized by the
     *                           SecurityManager
     */
    public static void useProxyUser( final String proxyHost, final String proxyPort, final String proxyUserName,
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
                                                           proxyPassword == null
                                                           ? new char[0] : proxyPassword.toCharArray() );
                    }
                } );
            }
        }
    }

    /**
     * Retrieve a remote file.  Throws an Exception on errors unless the
     * ifnoreErrors flag is set to True
     *
     * @param url             the of the file to retrieve
     * @param destinationFile where to store it
     * @param ignoreErrors    whether to ignore errors during I/O or throw an
     *                        exception when they happen
     * @param useTimestamp    whether to check the modified timestamp on the
     *                        <code>destinationFile</code> against the remote <code>source</code>
     * @param proxyHost       Proxy Host (if proxy is required), or null
     * @param proxyPort       Proxy Port (if proxy is required), or null
     * @param proxyUserName   Proxy Username (if authentification is required),
     *                        or null.
     * @param proxyPassword   Proxy Password (if authentification is required),
     *                        or null.
     * @param useChecksum     Flag to indicate the use of the checksum for the retrieved
     *                        artifact if it is available.
     * @throws IOException If an I/O exception occurs.
     */
    public static void getFile( String url, File destinationFile, boolean ignoreErrors, boolean useTimestamp,
                                String proxyHost, String proxyPort, String proxyUserName, String proxyPassword,
                                boolean useChecksum )
        throws IOException
    {
        // Get the requested file.
        getFile( url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort, proxyUserName, proxyPassword );

        // Get the checksum if requested.
        if ( useChecksum )
        {
            File checksumFile = new File( destinationFile + ".md5" );

            try
            {
                getFile( url + ".md5", checksumFile, ignoreErrors, useTimestamp, proxyHost, proxyPort, proxyUserName,
                         proxyPassword );
            }
            catch ( Exception e )
            {
                // do nothing we will check later in the process
                // for the checksums.
            }
        }
    }

    /**
     * Retrieve a remote file.  Throws an Exception on errors unless the
     * ifnoreErrors flag is set to True
     *
     * @param url             the of the file to retrieve
     * @param destinationFile where to store it
     * @param ignoreErrors    whether to ignore errors during I/O or throw an
     *                        exception when they happen
     * @param useTimestamp    whether to check the modified timestamp on the
     *                        <code>destinationFile</code> against the remote <code>source</code>
     * @param proxyHost       Proxy Host (if proxy is required), or null
     * @param proxyPort       Proxy Port (if proxy is required), or null
     * @param proxyUserName   Proxy Username (if authentification is required),
     *                        or null
     * @param proxyPassword   Proxy Password (if authentification is required),
     *                        or null
     * @throws IOException If an I/O exception occurs.
     */
    public static void getFile( String url, File destinationFile, boolean ignoreErrors, boolean useTimestamp,
                                String proxyHost, String proxyPort, String proxyUserName, String proxyPassword )
        throws IOException
    {
        //set the timestamp to the file date.
        long timestamp = -1;
        if ( useTimestamp && destinationFile.exists() )
        {
            timestamp = destinationFile.lastModified();
        }

        try
        {
            getFile( url, destinationFile, timestamp, proxyHost, proxyPort, proxyUserName, proxyPassword );
        }
        catch ( IOException ex )
        {
            if ( !ignoreErrors )
            {
                throw ex;
            }
        }
    }

    /**
     * Retrieve a remote file.
     *
     * @param url             the URL of the file to retrieve
     * @param destinationFile where to store it
     * @param timestamp       if provided, the remote URL is only retrieved if it was
     *                        modified more recently than timestamp. Otherwise, negative value indicates that
     *                        the remote URL should be retrieved unconditionally.
     * @param proxyHost       Proxy Host (if proxy is required), or null
     * @param proxyPort       Proxy Port (if proxy is required), or null
     * @param proxyUserName   Proxy Username (if authentification is required),
     *                        or null
     * @param proxyPassword   Proxy Password (if authentification is required),
     *                        or null
     * @throws IOException If an I/O exception occurs.
     */
    public static void getFile( String url, File destinationFile, long timestamp, String proxyHost, String proxyPort,
                                String proxyUserName, String proxyPassword )
        throws IOException
    {
        String[] s = parseUrl( url );
        String username = s[0];
        String password = s[1];
        String parsedUrl = s[2];

        URL source = new URL( parsedUrl );

        //set proxy connection
        useProxyUser( proxyHost, proxyPort, proxyUserName, proxyPassword );

        //set up the URL connection
        URLConnection connection = source.openConnection();

        //modify the headers
        if ( timestamp >= 0 )
        {
            connection.setIfModifiedSince( timestamp );
        }
        // prepare Java 1.1 style credentials
        if ( username != null || password != null )
        {
            String up = username + ":" + password;
            String encoding = Base64.encode( up.getBytes(), false );
            connection.setRequestProperty( "Authorization", "Basic " + encoding );
        }

        connection.setUseCaches( timestamp >= 0 );

        //connect to the remote site (may take some time)
        connection.connect();
        //next test for a 304 result (HTTP only)
        if ( connection instanceof HttpURLConnection )
        {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            // although HTTPUrlConnection javadocs says FileNotFoundException should be
            // thrown on a 404 error, that certainly does not appear to be the case, so
            // test for 404 ourselves, and throw FileNotFoundException as needed
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND )
            {
                throw new FileNotFoundException(
                    url.toString() + " (HTTP Error: " + httpConnection.getResponseCode() + " " +
                    httpConnection.getResponseMessage() +
                    ")" );
            }
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED )
            {
                return;
            }
            // test for 401 result (HTTP only)
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED )
            {
                throw new IOException( "Not authorized." );
            }
            // test for 407 result (HTTP only)
            if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH )
            {
                throw new IOException( "Not authorized by proxy." );
            }
        }

        // REVISIT: at this point even non HTTP connections may support the
        // if-modified-since behaviour - we just check the date of the
        // content and skip the write if it is not newer.
        // Some protocols (FTP) dont include dates, of course.

        InputStream is = null;
        IOException isException = null;
        for ( int i = 0; i < 3; i++ )
        {
            try
            {
                is = connection.getInputStream();
                break;
            }
            catch ( IOException ex )
            {
                isException = ex;
            }
        }
        if ( is == null )
        {
            throw isException;
        }

        if ( connection.getLastModified() <= timestamp && connection.getLastModified() != 0 )
        {
            return;
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
        if ( timestamp >= 0 )
        {
            long remoteTimestamp = connection.getLastModified();
            if ( remoteTimestamp != 0 )
            {
                touchFile( destinationFile, remoteTimestamp );
            }
        }
    }

    /**
     * Parse an url which might contain a username and password. If the
     * given url doesn't contain a username and password then return the
     * origin url unchanged.
     *
     * @param url The url to parse.
     * @return The username, password and url.
     * @throws RuntimeException if the url is (very) invalid
     */
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
            String protocol = url.substring( 0, url.indexOf( "://" ) ) + "://";
            String s = url.substring( protocol.length(), i );
            int j = s.indexOf( ":" );
            parsedUrl[0] = s.substring( 0, j );
            parsedUrl[1] = s.substring( j + 1 );
            parsedUrl[2] = protocol + url.substring( i + 1 );
        }

        return parsedUrl;
    }

    /**
     * set the timestamp of a named file to a specified time.
     *
     * @param file       the file to touch
     * @param timemillis in milliseconds since the start of the era
     * @return true if it succeeded. False means that this is a java1.1 system
     *         and that file times can not be set
     * @throws RuntimeException Thrown in unrecoverable error. Likely this
     *                          comes from file access failures.
     */
    private static boolean touchFile( File file, long timemillis )
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
