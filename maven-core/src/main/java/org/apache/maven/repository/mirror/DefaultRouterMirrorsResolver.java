package org.apache.maven.repository.mirror;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.repository.automirror.RouterMirrorSerializer;
import org.apache.maven.repository.automirror.RouterMirrors;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

@Component( role = RouterMirrorsResolver.class )
final class DefaultRouterMirrorsResolver
    implements Initializable, RouterMirrorsResolver
{

    private boolean initialized = false;

    @Requirement
    private Logger logger;

    @Requirement
    private AutoMirrorConfiguration config;

    @Requirement
    private EMBConfiguration embConfig;

    @Requirement( hint = "emb" )
    private Prompter prompter;

    private HttpClient client;

    /**
     * {@inheritDoc}
     * 
     * @see org.RouterMirrorsResolver.emb.mirror.resolve.MirrorsResolver#getMirrorMapping(java.lang.String)
     */
    @Override
    public RouterMirrors getMirrorMapping( final String routerUrl )
    {
        if ( config.isDisabled() )
        {
            return new RouterMirrors();
        }

        if ( routerUrl != null && !routerUrl.isEmpty() )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Grabbing mirror mappings from: " + routerUrl.toString() );
            }
            System.out.println( "Grabbing mirror mappings from: " + routerUrl );

            final HttpGet get = new HttpGet( routerUrl );
            get.addHeader( "Accept", "application/json;q=0.9,*/*;q=0.8" );

            try
            {
                return client.execute( get, new ResponseHandler<RouterMirrors>()
                {
                    public RouterMirrors handleResponse( final HttpResponse response )
                        throws /* ClientProtocolException, */IOException
                    {
                        final int statusCode = response.getStatusLine().getStatusCode();
                        if ( statusCode == 200 )
                        {
                            InputStream stream = null;
                            try
                            {
                                stream = response.getEntity().getContent();
                                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                IOUtil.copy( stream, baos );

                                String content = null;
                                final Header contentType = response.getFirstHeader( "Content-Type" );
                                if ( contentType != null )
                                {
                                    final HeaderElement[] contentTypeElts = contentType.getElements();

                                    if ( contentTypeElts != null )
                                    {
                                        for ( final HeaderElement elt : contentTypeElts )
                                        {
                                            final NameValuePair nv = elt.getParameterByName( "charset" );
                                            if ( nv != null )
                                            {
                                                content = new String( baos.toByteArray(), nv.getValue() );
                                            }
                                        }
                                    }
                                }

                                if ( content == null )
                                {
                                    content = new String( baos.toByteArray() );
                                }

                                if ( logger.isDebugEnabled() )
                                {
                                    logger.debug( "Response code/message: '" + response.getStatusLine().getStatusCode()
                                                    + " " + response.getStatusLine().getReasonPhrase()
                                                    + "'\nContent is:\n\n" + content );
                                }

                                return RouterMirrorSerializer.deserialize( content );
                            }
                            catch ( final JsonParseException e )
                            {
                                logger.error( "Failed to retrieve mirror mapping from: " + routerUrl, e );
                            }
                            finally
                            {
                                close( stream );
                            }
                        }
                        else if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "Response: " + response.getStatusLine().getStatusCode() + " "
                                            + response.getStatusLine().getReasonPhrase() );
                        }

                        return null;
                    }
                } );
            }
            catch ( final ClientProtocolException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Failed to read proxied repositories from: '" + routerUrl + "'. Reason: "
                                                  + e.getMessage(), e );
                }
            }
            catch ( final IOException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Failed to read proxied repositories from: '" + routerUrl + "'. Reason: "
                                                  + e.getMessage(), e );
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable#initialize()
     */
    @Override
    public synchronized void initialize()
        throws InitializationException
    {
        if ( initialized )
        {
            return;
        }

        final DefaultHttpClient client = new DefaultHttpClient();
        this.client = client;

        client.setCredentialsProvider( new CredentialsProvider()
        {
            private final Map<String, UsernamePasswordCredentials> cached =
                new HashMap<String, UsernamePasswordCredentials>();

            public void setCredentials( final AuthScope authscope, final Credentials credentials )
            {
            }

            public synchronized Credentials getCredentials( final AuthScope authscope )
            {
                UsernamePasswordCredentials creds = config.getRouterCredentials();
                if ( creds == null && embConfig.isInteractive() )
                {
                    final String key = authscope.getHost() + ":" + authscope.getPort();

                    creds = cached.get( key );
                    if ( creds == null )
                    {
                        final StringBuilder sb =
                            new StringBuilder().append( authscope.getRealm() )
                                               .append( " (" )
                                               .append( key )
                                               .append( ") requires authentication.\n\nUsername" );

                        try
                        {
                            final String user = prompter.prompt( sb.toString() );
                            final String password = prompter.promptForPassword( "Password" );

                            creds = new UsernamePasswordCredentials( user, password );
                            cached.put( key, creds );
                        }
                        catch ( final PrompterException e )
                        {
                            if ( logger.isDebugEnabled() )
                            {
                                logger.debug( "Failed to read credentials! Reason: " + e.getMessage(), e );
                            }
                        }
                    }
                }

                return creds;
            }

            public void clear()
            {
            }
        } );

        initialized = true;
    }
}
