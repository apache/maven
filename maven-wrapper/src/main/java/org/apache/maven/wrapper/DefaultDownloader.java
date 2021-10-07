package org.apache.maven.wrapper;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_PASSWORD;
import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_USERNAME;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author Hans Dockter
 */
public class DefaultDownloader
    implements Downloader
{
    private static final int PROGRESS_CHUNK = 500000;

    private static final int BUFFER_SIZE = 10000;

    private final String applicationName;

    private final String applicationVersion;

    public DefaultDownloader( String applicationName, String applicationVersion )
    {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        configureProxyAuthentication();
        configureAuthentication();
    }

    private void configureProxyAuthentication()
    {
        if ( System.getProperty( "http.proxyUser" ) != null )
        {
            Authenticator.setDefault( new SystemPropertiesProxyAuthenticator() );
        }
    }

    private void configureAuthentication()
    {
        if ( System.getenv( MVNW_USERNAME ) != null && System.getenv( MVNW_PASSWORD ) != null
            && System.getProperty( "http.proxyUser" ) == null )
        {
            Authenticator.setDefault( new Authenticator()
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication( System.getenv( MVNW_USERNAME ),
                                                       System.getenv( MVNW_PASSWORD ).toCharArray() );
                }
            } );
        }
    }

    @Override
    public void download( URI address, Path destination ) throws IOException
    {
        if ( Files.exists( destination ) )
        {
            return;
        }
        Files.createDirectories( destination.getParent() );

        downloadInternal( address, destination );
    }

    private void downloadInternal( URI address, Path destination ) throws IOException
    {
        URL url = address.toURL();
        URLConnection conn = url.openConnection();
        addBasicAuthentication( address, conn );
        final String userAgentValue = calculateUserAgent();
        conn.setRequestProperty( "User-Agent", userAgentValue );

        try ( OutputStream out = new BufferedOutputStream( Files.newOutputStream( destination ) );
              InputStream in = conn.getInputStream() )
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int numRead;
            long progressCounter = 0;
            while ( ( numRead = in.read( buffer ) ) != -1 )
            {
                progressCounter += numRead;
                if ( progressCounter / PROGRESS_CHUNK > 0 )
                {
                    Logger.info( "." );
                    progressCounter = progressCounter - PROGRESS_CHUNK;
                }
                out.write( buffer, 0, numRead );
            }
        }
        finally
        {
            Logger.info( "" );
        }
    }

    private void addBasicAuthentication( URI address, URLConnection connection )
        throws IOException
    {
        String userInfo = calculateUserInfo( address );
        if ( userInfo == null )
        {
            return;
        }
        if ( !"https".equals( address.getScheme() ) )
        {
            Logger.warn( "WARNING Using HTTP Basic Authentication over an insecure connection"
                + " to download the Maven distribution. Please consider using HTTPS." );
        }
        connection.setRequestProperty( "Authorization", "Basic " + base64Encode( userInfo ) );
    }

    /**
     * Base64 encode user info for HTTP Basic Authentication.
     *
     * @param userInfo user info
     * @return Base64 encoded user info
     * @throws RuntimeException if no public Base64 encoder is available on this JVM
     */
    private String base64Encode( String userInfo )
    {
        return Base64.getEncoder().encodeToString( userInfo.getBytes( StandardCharsets.UTF_8 ) );
    }

    private String calculateUserInfo( URI uri )
    {
        String username = System.getenv( MVNW_USERNAME );
        String password = System.getenv( MVNW_PASSWORD );
        if ( username != null && password != null )
        {
            return username + ':' + password;
        }
        return uri.getUserInfo();
    }

    private String calculateUserAgent()
    {
        String javaVendor = System.getProperty( "java.vendor" );
        String javaVersion = System.getProperty( "java.version" );
        String javaVendorVersion = System.getProperty( "java.vm.version" );
        String osName = System.getProperty( "os.name" );
        String osVersion = System.getProperty( "os.version" );
        String osArch = System.getProperty( "os.arch" );
        return String.format( "%s/%s (%s;%s;%s) (%s;%s;%s)", applicationName, applicationVersion, osName, osVersion,
                osArch, javaVendor, javaVersion, javaVendorVersion );
    }

    private static class SystemPropertiesProxyAuthenticator
        extends Authenticator
    {
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication( System.getProperty( "http.proxyUser" ),
                                               System.getProperty( "http.proxyPassword", "" ).toCharArray() );
        }
    }
}
