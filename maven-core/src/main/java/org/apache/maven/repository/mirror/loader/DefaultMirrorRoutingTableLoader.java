/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.mirror.loader;

import static org.codehaus.plexus.util.IOUtil.close;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.repository.automirror.MirrorRouteSerializer;
import org.apache.maven.repository.automirror.MirrorRouterModelException;
import org.apache.maven.repository.automirror.MirrorRoutingTable;
import org.apache.maven.repository.mirror.MirrorRouterException;
import org.apache.maven.repository.mirror.configuration.MirrorRouterConfiguration;
import org.apache.maven.repository.mirror.discovery.RouterDiscoveryStrategy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component( role = MirrorRoutingTableLoader.class )
public class DefaultMirrorRoutingTableLoader
    implements MirrorRoutingTableLoader
{

    @Requirement( role = RouterDiscoveryStrategy.class )
    private Map<String, RouterDiscoveryStrategy> strategies;

    @Requirement
    private Logger logger;

    public MirrorRoutingTable load( final MirrorRouterConfiguration config )
        throws MirrorRouterException
    {
        MirrorRoutingTable routingTable = null;

        if ( !config.isDisabled() )
        {
            final DefaultHttpClient client = new DefaultHttpClient();
            if ( config.getRouterCredentials() != null )
            {
                client.setCredentialsProvider( new CredentialsProvider()
                {
                    public void setCredentials( final AuthScope authscope, final Credentials credentials )
                    {
                    }

                    public synchronized Credentials getCredentials( final AuthScope authscope )
                    {
                        final UsernamePasswordCredentials creds = config.getRouterCredentials();
                        return creds;
                    }

                    public void clear()
                    {
                    }
                } );
            }

            try
            {
                if ( config.getRouterUrl() != null )
                {
                    routingTable = getMirrorMapping( config.getRouterUrl(), config, client );
                }
                else
                {
                    final String[] discoStrategies = config.getDiscoveryStrategies();
                    final List<RouterDiscoveryStrategy> strats = new ArrayList<RouterDiscoveryStrategy>();
                    if ( discoStrategies.length == 1 )
                    {
                        final String key = discoStrategies[0];
                        if ( MirrorRouterConfiguration.NO_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
                        {
                            // NOP
                        }
                        else if ( MirrorRouterConfiguration.ALL_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
                        {
                            strats.addAll( strategies.values() );
                        }
                        else
                        {
                            final RouterDiscoveryStrategy strat = getDiscoveryStrategy( key );
                            if ( strat != null )
                            {
                                strats.add( strat );
                            }
                        }
                    }
                    else
                    {
                        for ( final String key : discoStrategies )
                        {
                            final RouterDiscoveryStrategy strat = getDiscoveryStrategy( key );
                            if ( strat != null )
                            {
                                strats.add( strat );
                            }
                        }
                    }

                    String routerUrl = null;
                    for ( final RouterDiscoveryStrategy strategy : strats )
                    {
                        routerUrl = strategy.findRouter();
                        if ( routerUrl != null && routerUrl.trim().length() > 0 )
                        {
                            routingTable = getMirrorMapping( routerUrl, config, client );
                            if ( routingTable != null )
                            {
                                break;
                            }
                        }
                    }
                }

                final String centralRouterUrl = config.getCanonicalRouterUrl();
                if ( routingTable == null && centralRouterUrl != null && centralRouterUrl.trim().length() > 0 )
                {
                    routingTable = getMirrorMapping( centralRouterUrl, config, client );
                }
            }
            catch ( final MirrorRouterException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.error( "Failed to auto-detect mirrors: " + e.getMessage(), e );
                }
            }
        }

        if ( routingTable == null )
        {
            routingTable = new MirrorRoutingTable();
        }

        return routingTable;
    }

    private RouterDiscoveryStrategy getDiscoveryStrategy( final String key )
    {
        RouterDiscoveryStrategy strat = strategies.get( key );
        if ( strat == null )
        {
            strat = strategies.get( key.toLowerCase() );
        }

        if ( strat == null )
        {
            logger.warn( "Cannot find RouterDiscoveryStrategy with hint: '" + key + "'" );
        }

        return strat;
    }

    private MirrorRoutingTable getMirrorMapping( final String routerUrl, final MirrorRouterConfiguration config,
                                                 final HttpClient client )
    {
        if ( config.isDisabled() )
        {
            return new MirrorRoutingTable();
        }

        if ( routerUrl != null && routerUrl.trim().length() > 0 )
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
                return client.execute( get, new ResponseHandler<MirrorRoutingTable>()
                {
                    public MirrorRoutingTable handleResponse( final HttpResponse response )
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

                                return MirrorRouteSerializer.deserialize( content );
                            }
                            catch ( final MirrorRouterModelException e )
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
}
