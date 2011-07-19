/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.artifact.router.loader;

import static org.codehaus.plexus.util.IOUtil.close;
import static org.codehaus.plexus.util.StringUtils.isNotBlank;

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.ArtifactRouterException;
import org.apache.maven.artifact.router.conf.RouterSource;
import org.apache.maven.artifact.router.io.ArtifactRouteSerializer;
import org.apache.maven.artifact.router.session.ArtifactRouterSession;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.component.annotations.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

@Component( role = ArtifactRouterReader.class )
public class DefaultArtifactRouterReader
    implements ArtifactRouterReader
{

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.artifact.router.loader.ArtifactRouterReader#loadRouter(java.lang.String,
     *      org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration)
     */
    public ArtifactRouter loadRouter( RouterSource source, ArtifactRouterSession session )
        throws ArtifactRouterException
    {
        RouterAuthenticator authenticator = new RouterAuthenticator( source, session );
        Authenticator.setDefault( authenticator );

        final URL url;
        try
        {
            url = new URL( source.getUrl() );
        }
        catch ( MalformedURLException e )
        {
            throw new ArtifactRouterException( "Invalid router source: '" + source + "'", e );
        }

        Proxy proxy = authenticator.getProxy( url.getProtocol(), url.getHost() );

        ArtifactRouter router = null;
        InputStream in = null;
        try
        {
            URLConnection conn;
            if ( proxy == null )
            {
                conn = url.openConnection();
            }
            else
            {
                Type type = proxy.getProtocol().toLowerCase().startsWith( "socks" ) ? Type.SOCKS : Type.HTTP;
                
                java.net.Proxy connProxy =
                    new java.net.Proxy( type, new InetSocketAddress( proxy.getHost(), proxy.getPort() ) );

                conn = url.openConnection( connProxy );
            }

            conn.setRequestProperty( "Accept", "application/json;q=0.9,*/*;q=0.8" );
            conn.setRequestProperty( "Accept-Encoding", "gzip" );

            in = conn.getInputStream();
            String contentEncoding = conn.getHeaderField( "Content-Encoding" );
            boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase( contentEncoding );
            if ( isGZipped )
            {
                in = new GZIPInputStream( in );
            }

            router = ArtifactRouteSerializer.deserialize( new InputStreamReader( in ) );
        }
        catch ( IOException e )
        {
            throw new ArtifactRouterException( "Cannot load artifact routes from: " + source, e );
        }
        finally
        {
            close( in );
        }

        return router;
    }

    // private Proxy setProxy( Proxy proxy )
    // {
    // Proxy prev = new Proxy();
    // prev.setHost( System.getProperty( "http.proxyHost" ) );
    // prev.setNonProxyHosts( System.getProperty( "http.nonProxyHosts" ) );
    //
    // String port = System.getProperty( "http.proxyPort" );
    // if ( port != null )
    // {
    // prev.setPort( Integer.parseInt( port ) );
    // }
    //
    // if ( proxy != null )
    // {
    // setSystemProperty( "http.proxyHost", proxy.getHost() );
    // setSystemProperty( "http.proxyPort", String.valueOf( proxy.getPort() ) );
    // setSystemProperty( "http.nonProxyHosts", proxy.getNonProxyHosts() );
    // }
    // else
    // {
    // setSystemProperty( "http.proxyHost", null );
    // setSystemProperty( "http.proxyPort", null );
    // }
    //
    // return prev;
    // }

    void setSystemProperty( String key, String value )
    {
        if ( value != null )
        {
            System.setProperty( key, value );
        }
        else
        {
            System.getProperties().remove( key );
        }
    }

    private static final class RouterAuthenticator
        extends Authenticator
    {

        private final ArtifactRouterSession session;

        private final RouterSource source;

        public RouterAuthenticator( RouterSource source, ArtifactRouterSession session )
        {
            this.source = source;
            this.session = session;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            if ( getRequestorType() == RequestorType.PROXY )
            {
                Proxy proxy = getProxy();
                if ( proxy != null )
                {
                    String user = proxy.getUsername();
                    String password = proxy.getPassword();
                    if ( isNotBlank( user ) && isNotBlank( password ) )
                    {
                        return new PasswordAuthentication( user, password.toCharArray() );
                    }
                }
            }
            else
            {
                Server server = session.getServer( source.getId() );
                if ( server != null )
                {
                    return new PasswordAuthentication( server.getUsername(), server.getPassword().toCharArray() );
                }
            }

            return null;
        }

        private Proxy getProxy()
        {
            return getProxy( getRequestingProtocol(), getRequestingHost() );
        }

        Proxy getProxy( String protocol, String host )
        {
            Proxy proxy = session.getProxy( getRequestingProtocol() );
            if ( proxy != null && !checkNonProxyHosts( proxy, getRequestingHost() ) )
            {
                return proxy;
            }

            return null;
        }

        @Override
        protected URL getRequestingURL()
        {
            try
            {
                if ( getRequestorType() == RequestorType.PROXY )
                {
                    Proxy proxy = getProxy();
                    if ( proxy != null )
                    {
                        return new URL( proxy.getProtocol() + "://" + proxy.getHost()
                            + ( proxy.getPort() > 0 ? ":" + proxy.getPort() : "" ) );
                    }
                }
                else
                {
                    return new URL( source.getUrl() );
                }
            }
            catch ( MalformedURLException e )
            {
            }

            return null;
        }

        private static boolean checkNonProxyHosts( Proxy proxy, String targetHost )
        {
            if ( targetHost == null )
            {
                targetHost = new String();
            }
            if ( proxy == null )
            {
                return false;
            }
            String nonProxyHosts = proxy.getNonProxyHosts();
            if ( nonProxyHosts == null )
            {
                return false;
            }

            StringTokenizer tokenizer = new StringTokenizer( nonProxyHosts, "|" );

            while ( tokenizer.hasMoreTokens() )
            {
                String pattern = tokenizer.nextToken();
                pattern = pattern.replaceAll( "\\.", "\\\\." ).replaceAll( "\\*", ".*" );
                if ( targetHost.matches( pattern ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

}
