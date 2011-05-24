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

package org.apache.maven.configuration.mirror;

import static org.codehaus.plexus.util.IOUtil.close;
import static org.codehaus.plexus.util.StringUtils.isNotBlank;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Component( role = AutoMirrorConfiguration.class )
public class AutoMirrorConfiguration
    implements Contextualizable
{

    private static final String AUTOMIRROR_CONFIG_FILENAME = "automirror.properties";

    private static final String KEY_ROUTER_URL = "router-url";

    private static final String KEY_ROUTER_USER = "router-user";

    private static final String KEY_ROUTER_PASSWORD = "router-password";

    private static final String KEY_DISABLED = "disabled";

    private static final String KEY_DISCOVERY_STRATEGIES = "discovery-strategies";

    private static final String CANONICAL_ROUTER_URL = "http://repository.apache.org/router/mirrors.json";

    public static final String ALL_DISCOVERY_STRATEGIES = "all";

    public static final String NO_DISCOVERY_STRATEGIES = "none";

    private String routerUrl;

    private UsernamePasswordCredentials routerCredentials;

    private boolean disabled = false;

    private String[] discoveryStrategies = { ALL_DISCOVERY_STRATEGIES };

    @Requirement
    private Logger logger;

    public AutoMirrorConfiguration withRouterCredentials( final String user, final String password )
    {
        routerCredentials = new UsernamePasswordCredentials( user, password );
        return this;
    }

    public UsernamePasswordCredentials getRouterCredentials()
    {
        return routerCredentials;
    }

    public AutoMirrorConfiguration withRouterUrl( final String routerUrl )
    {
        this.routerUrl = routerUrl;
        return this;
    }

    public String getRouterUrl()
    {
        return routerUrl;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public AutoMirrorConfiguration setDisabled( final boolean disabled )
    {
        this.disabled = disabled;
        return this;
    }

    public String getCanonicalRouterUrl()
    {
        return CANONICAL_ROUTER_URL;
    }

    public String[] getDiscoveryStrategies()
    {
        return discoveryStrategies == null ? new String[0] : discoveryStrategies;
    }

    public AutoMirrorConfiguration setDiscoveryStrategies( final String... strategies )
    {
        discoveryStrategies = strategies;
        return this;
    }

    public void contextualize( final Context context )
    {
        final File configFile = new File( embConfig.getConfigurationDirectory(), AUTOMIRROR_CONFIG_FILENAME );
        final AutoMirrorConfiguration config = new AutoMirrorConfiguration();

        if ( configFile.exists() )
        {
            logger.info( "Loading configuration file: " + configFile );
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( configFile );
                final Properties p = new Properties();
                p.load( stream );

                config.withRouterUrl( p.getProperty( KEY_ROUTER_URL ) )
                      .setDisabled( Boolean.parseBoolean( p.getProperty( KEY_DISABLED, "false" ) ) );

                final String user = p.getProperty( KEY_ROUTER_USER );
                final String pass = p.getProperty( KEY_ROUTER_PASSWORD );

                final String[] strat =
                    p.getProperty( KEY_DISCOVERY_STRATEGIES, AutoMirrorConfiguration.ALL_DISCOVERY_STRATEGIES )
                     .split( "\\s*,\\s*" );

                config.setDiscoveryStrategies( strat );

                if ( isNotBlank( user ) && isNotBlank( pass ) )
                {
                    config.withRouterCredentials( user, pass );
                }
            }
            catch ( final IOException e )
            {
                throw new ExtensionConfigurationException( "Failed to read properties: '{0}' from: {1}\nReason: {2}",
                                                           e, AUTOMIRROR_CONFIG_FILENAME,
                                                           embConfig.getConfigurationDirectory(), e.getMessage() );
            }
            finally
            {
                close( stream );
            }
        }
        else
        {
            logger.info( "Cannot find configuration file: " + configFile + ". Using defaults." );
        }
    }

}
