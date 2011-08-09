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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.ArtifactRouterException;
import org.apache.maven.artifact.router.DefaultArtifactRouter;
import org.apache.maven.artifact.router.WeightedRandomRouteSelector;
import org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration;
import org.apache.maven.artifact.router.conf.RouterSource;
import org.apache.maven.artifact.router.io.ArtifactRouteSerializer;
import org.apache.maven.artifact.router.io.ArtifactRouterModelException;
import org.apache.maven.artifact.router.loader.discovery.ArtifactRouterDiscoveryStrategy;
import org.apache.maven.artifact.router.session.ArtifactRouterSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component( role = ArtifactRouterLoader.class )
public class DefaultRouterLoader
    implements ArtifactRouterLoader
{

    @Requirement( role = ArtifactRouterDiscoveryStrategy.class )
    private Map<String, ArtifactRouterDiscoveryStrategy> strategies;

    @Requirement
    private Logger logger;
    
    @Requirement
    private ArtifactRouterReader routerReader;

    public ArtifactRouter load( final ArtifactRouterSession session )
        throws ArtifactRouterException
    {
        if ( session == null || session.isDisabled() )
        {
            // FIXME
            throw new UnsupportedOperationException();
//            return new DefaultArtifactRouter( new WeightedRandomRouteSelector(), null, null );
        }
        
        return loadRoutes( session );
    }

    public void save( final ArtifactRouter router, final ArtifactRouterSession session )
        throws ArtifactRouterException
    {
        if ( session == null )
        {
            return;
        }
        
        File routingTablesFile = session.getRoutesFile();
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

    protected ArtifactRouter loadSaved( ArtifactRouterSession session )
        throws ArtifactRouterException
    {
        File routesFile = session.getRoutesFile();
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
        
        // FIXME
        throw new UnsupportedOperationException();
//        return new DefaultArtifactRouter();
    }

    protected ArtifactRouter loadRoutes( final ArtifactRouterSession session )
        throws ArtifactRouterException
    {
        ArtifactRouter routes = null;
        
        if ( session.isClear() )
        {
            // FIXME
//            routes = new DefaultArtifactRouter();
            save( routes, session );
            throw new UnsupportedOperationException();
        }
        else
        {
            routes = loadSaved( session );
        }

        if ( session.isUpdate() && !session.isOffline() )
        {
            try
            {
                final String discoveryStrategy = session.getDiscoveryStrategy();
                RouterSource src = session.getSource();

                if ( session.getSource() != null )
                {
                    ArtifactRouter r = routerReader.loadRouter( src, session );
                    if ( r != null )
                    {
                        routes = merge( routes, r );
                    }
                }
                else if ( discoveryStrategy != null
                    && !ArtifactRouterConfiguration.NO_DISCOVERY_STRATEGIES.equalsIgnoreCase( discoveryStrategy.toLowerCase() ) )
                {
                    final String key = discoveryStrategy;
                    final ArtifactRouterDiscoveryStrategy strategy = getDiscoveryStrategy( key );
                    if ( strategy == null )
                    {
                        if ( logger.isDebugEnabled() )
                        {
                            logger.error( "Router discovery strategy cannot be found: " + discoveryStrategy );
                        }
                    }
                    else
                    {
                        ArtifactRouter result = strategy.findRouter( session );
                        if ( result != null )
                        {
                            routes = merge( routes, result );
                        }
                    }
                }
                else
                {
                    src = session.getDefaultSource();
                    ArtifactRouter r = routerReader.loadRouter( src, session );
                    if ( r != null )
                    {
                        routes = merge( routes, r );
                    }
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
            // FIXME
            throw new UnsupportedOperationException();
//            routes = new DefaultArtifactRouter();
        }

        return routes;
    }

    private ArtifactRouter merge( ArtifactRouter routes, ArtifactRouter r )
    {
        // FIXME
        throw new UnsupportedOperationException();
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

    public DefaultArtifactRouter loadDefault()
    {
        // FIXME Implement this!
        throw new UnsupportedOperationException( "TODO: Implement me!" );
    }

}
