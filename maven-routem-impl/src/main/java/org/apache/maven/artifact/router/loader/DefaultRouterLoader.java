package org.apache.maven.artifact.router.loader;

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
import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.ArtifactRouterException;
import org.apache.maven.artifact.router.GroupRoute;
import org.apache.maven.artifact.router.MirrorRoute;
import org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration;
import org.apache.maven.artifact.router.discovery.ArtifactRouterDiscoveryStrategy;
import org.apache.maven.artifact.router.io.ArtifactRouteSerializer;
import org.apache.maven.artifact.router.io.ArtifactRouterModelException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component( role = ArtifactRouterLoader.class )
public class DefaultRouterLoader
    implements ArtifactRouterLoader
{

    @Requirement( role = ArtifactRouterDiscoveryStrategy.class )
    private Map<String, ArtifactRouterDiscoveryStrategy> strategies;

    @Requirement
    private Logger logger;

    public ArtifactRouter load( final ArtifactRouterConfiguration config )
        throws ArtifactRouterException
    {
        if ( config == null || config.isDisabled() )
        {
            return new ArtifactRouter();
        }
        
        return loadRoutes( config );
    }

    public void save( final ArtifactRouter router, final ArtifactRouterConfiguration config )
        throws ArtifactRouterException
    {
        if ( config == null )
        {
            return;
        }
        
        File routingTablesFile = config.getRoutesFile();
        if ( routingTablesFile != null )
        {
            FileWriter writer = null;
            try
            {
                File dir = routingTablesFile.getParentFile();
                if ( dir != null && !dir.exists() )
                {
                    dir.mkdirs();
                }
                
                writer = new FileWriter( routingTablesFile );
                ArtifactRouteSerializer.serialize( router, writer );
            }
            catch ( IOException e )
            {
                throw new ArtifactRouterException( "Cannot write artifact router to: " + routingTablesFile, e );
            }
            catch ( ArtifactRouterModelException e )
            {
                throw new ArtifactRouterException( "Cannot write artifact router to: " + routingTablesFile, e );
            }
            finally
            {
                close( writer );
            }
        }
    }

    protected ArtifactRouter loadSaved( ArtifactRouterConfiguration config )
        throws ArtifactRouterException
    {
        File routesFile = config.getRoutesFile();
        if ( routesFile != null && routesFile.exists() && routesFile.canRead() )
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader( routesFile );
                return ArtifactRouteSerializer.deserialize( reader );
            }
            catch ( IOException e )
            {
                throw new ArtifactRouterException( "Cannot read saved route information from: " + routesFile, e );
            }
            catch ( ArtifactRouterModelException e )
            {
                throw new ArtifactRouterException( "Cannot read saved route information from: " + routesFile, e );
            }
            finally
            {
                close( reader );
            }
        }
        
        return new ArtifactRouter();
    }

    protected ArtifactRouter loadRoutes( final ArtifactRouterConfiguration config )
        throws ArtifactRouterException
    {
        ArtifactRouter routes = null;
        
        if ( config.isClear() )
        {
            routes = new ArtifactRouter();
            save( routes, config );
        }
        else
        {
            routes = loadSaved( config );
        }

        if ( config.isUpdate() && !config.isOffline() )
        {
            // FIXME: What about proxies?! Should be using settings.xml proxy defs.
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
                if ( config.getRouterMirrorsUrl() != null )
                {
                    routes.addMirrors( getMirrorMapping( config.getRouterMirrorsUrl(), config, client ) );
                }
                
                if ( config.getRouterGroupsUrl() != null )
                {
                    routes.addGroups( getGroupMapping( config.getRouterGroupsUrl(), config, client ) );
                }
                
                final String[] discoStrategies = config.getDiscoveryStrategies();
                if ( discoStrategies != null && discoStrategies.length > 0
                    && !"none".equalsIgnoreCase( discoStrategies[0].toLowerCase() ) )
                {
                    final List<ArtifactRouterDiscoveryStrategy> strats = new ArrayList<ArtifactRouterDiscoveryStrategy>();
                    if ( discoStrategies.length == 1 )
                    {
                        final String key = discoStrategies[0];
                        if ( ArtifactRouterConfiguration.NO_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
                        {
                            // NOP
                        }
                        else if ( ArtifactRouterConfiguration.ALL_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
                        {
                            strats.addAll( strategies.values() );
                        }
                        else
                        {
                            final ArtifactRouterDiscoveryStrategy strat = getDiscoveryStrategy( key );
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
                            final ArtifactRouterDiscoveryStrategy strat = getDiscoveryStrategy( key );
                            if ( strat != null )
                            {
                                strats.add( strat );
                            }
                        }
                    }

                    for ( final ArtifactRouterDiscoveryStrategy strategy : strats )
                    {
                        ArtifactRouter result = strategy.findRouter();
                        if ( result != null )
                        {
                            routes.merge( result );
                        }
                    }
                }

                final String centralMirrorsUrl = config.getCanonicalMirrorsUrl();
                if ( centralMirrorsUrl != null && centralMirrorsUrl.trim().length() > 0 )
                {
                    routes.addMirrors( getMirrorMapping( centralMirrorsUrl, config, client ) );
                }
                
                final String centralGroupsUrl = config.getCanonicalGroupsUrl();
                if ( centralGroupsUrl != null && centralGroupsUrl.trim().length() > 0 )
                {
                    routes.addGroups( getGroupMapping( centralGroupsUrl, config, client ) );
                }
            }
            catch ( final ArtifactRouterException e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.error( "Failed to auto-detect routing information: " + e.getMessage(), e );
                }
            }
        }

        if ( routes == null )
        {
            routes = new ArtifactRouter();
        }

        return routes;
    }

    private ArtifactRouterDiscoveryStrategy getDiscoveryStrategy( final String key )
    {
        ArtifactRouterDiscoveryStrategy strat = strategies.get( key );
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

    private Set<MirrorRoute> getMirrorMapping( final String routerUrl, final ArtifactRouterConfiguration config,
                                                 final HttpClient client )
    {
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
                return client.execute( get, new ResponseHandler<Set<MirrorRoute>>()
                {
                    public Set<MirrorRoute> handleResponse( final HttpResponse response )
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
                                        + " " + response.getStatusLine().getReasonPhrase() + "'\nContent is:\n\n"
                                        + content );
                                }

                                return ArtifactRouteSerializer.deserializeMirrors( content );
                            }
                            catch ( final ArtifactRouterModelException e )
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

    private Set<GroupRoute> getGroupMapping( final String routerUrl, final ArtifactRouterConfiguration config,
                                               final HttpClient client )
  {
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
              return client.execute( get, new ResponseHandler<Set<GroupRoute>>()
              {
                  public Set<GroupRoute> handleResponse( final HttpResponse response )
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
                                      + " " + response.getStatusLine().getReasonPhrase() + "'\nContent is:\n\n"
                                      + content );
                              }

                              return ArtifactRouteSerializer.deserializeGroups( content );
                          }
                          catch ( final ArtifactRouterModelException e )
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
