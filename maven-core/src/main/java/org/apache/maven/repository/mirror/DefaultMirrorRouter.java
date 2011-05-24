/*
 * Copyright 2010 Red Hat, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.repository.mirror;

import org.apache.log4j.Logger;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.commonjava.emb.conf.EMBLibrary;
import org.commonjava.emb.mirror.AutoMirrorException;
import org.commonjava.emb.mirror.conf.AutoMirrorConfiguration;
import org.commonjava.emb.mirror.conf.AutoMirrorLibrary;
import org.commonjava.emb.mirror.discovery.RouterDiscoveryStrategy;
import org.commonjava.emb.mirror.model.RouterMirror;
import org.commonjava.emb.mirror.model.RouterMirrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component( role = MirrorRouter.class )
final class DefaultMirrorRouter
    implements Initializable, MirrorRouter
{

    private RouterMirrors mirrorMapping;

    private boolean initialized = false;

    @Requirement( hint = AutoMirrorLibrary.HINT )
    private EMBLibrary library;

    @Requirement
    private AutoMirrorConfiguration config;

    @Requirement( role = RouterDiscoveryStrategy.class )
    private Map<String, RouterDiscoveryStrategy> strategies;

    @Requirement
    private RouterMirrorsResolver mirrorResolver;

    public synchronized void initialize()
        throws InitializationException
    {
        if ( initialized )
        {
            return;
        }

        if ( !config.isDisabled() )
        {
            try
            {
                if ( config.getRouterUrl() != null )
                {
                    mirrorMapping = mirrorResolver.getMirrorMapping( config.getRouterUrl() );
                }
                else
                {
                    final String[] discoStrategies = config.getDiscoveryStrategies();
                    final List<RouterDiscoveryStrategy> strats = new ArrayList<RouterDiscoveryStrategy>();
                    if ( discoStrategies.length == 1 )
                    {
                        final String key = discoStrategies[0];
                        if ( AutoMirrorConfiguration.NO_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
                        {
                            // NOP
                        }
                        else if ( AutoMirrorConfiguration.ALL_DISCOVERY_STRATEGIES.equalsIgnoreCase( key ) )
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
                        if ( routerUrl != null && !routerUrl.trim().isEmpty() )
                        {
                            mirrorMapping = mirrorResolver.getMirrorMapping( routerUrl );
                            if ( mirrorMapping != null )
                            {
                                break;
                            }
                        }
                    }
                }

                final String centralRouterUrl = config.getCanonicalRouterUrl();
                if ( mirrorMapping == null && centralRouterUrl != null && centralRouterUrl.trim().length() > 0 )
                {
                    mirrorMapping = mirrorResolver.getMirrorMapping( config.getRouterUrl() );
                }
            }
            catch ( final AutoMirrorException e )
            {
                if ( library.getLogger().isDebugEnabled() )
                {
                    library.getLogger().error( "Failed to auto-detect Nexus mirrors: " + e.getMessage(), e );
                }
            }
        }

        if ( mirrorMapping == null )
        {
            mirrorMapping = new RouterMirrors();
        }

        initialized = true;
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
            library.getLogger().warn( "Cannot find RouterDiscoveryStrategy with hint: '" + key + "'" );
        }

        return strat;
    }

    protected Logger getLogger()
    {
        return library.getLogger();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.commonjava.emb.mirror.resolve.MirrorRouter#getWeightedRandomSuggestion(java.lang.String)
     */
    @Override
    public RouterMirror getWeightedRandomSuggestion( final String canonicalUrl )
    {
        return mirrorMapping.getWeightedRandomSuggestion( canonicalUrl );
    }

}
