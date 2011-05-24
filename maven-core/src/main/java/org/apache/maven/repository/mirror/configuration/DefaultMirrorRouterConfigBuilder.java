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

package org.apache.maven.repository.mirror.configuration;

import static org.codehaus.plexus.util.IOUtil.close;
import static org.codehaus.plexus.util.StringUtils.isNotBlank;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component( role = MirrorRouterConfigBuilder.class )
public class DefaultMirrorRouterConfigBuilder
    implements MirrorRouterConfigBuilder
{

    private static final String KEY_ROUTER_URL = "router-url";

    private static final String KEY_ROUTER_USER = "router-user";

    private static final String KEY_ROUTER_PASSWORD = "router-password";

    private static final String KEY_DISABLED = "disabled";

    private static final String KEY_DISCOVERY_STRATEGIES = "discovery-strategies";

    @Requirement
    private Logger logger;

    public MirrorRouterConfiguration build( final MirrorRouterConfigSource source )
        throws MirrorRouterConfigurationException
    {
        final MirrorRouterConfiguration config = new MirrorRouterConfiguration();

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Loading router-mirror configuration from file: " + source.getSource() );
        }

        if ( source.canRead() )
        {
            InputStream stream = null;
            try
            {
                stream = source.getInputStream();
                final Properties p = new Properties();
                p.load( stream );

                config.setRouterUrl( p.getProperty( KEY_ROUTER_URL ) );
                config.setDisabled( Boolean.parseBoolean( p.getProperty( KEY_DISABLED, "false" ) ) );

                final String user = p.getProperty( KEY_ROUTER_USER );
                final String pass = p.getProperty( KEY_ROUTER_PASSWORD );

                final String[] strat =
                    p.getProperty( KEY_DISCOVERY_STRATEGIES, MirrorRouterConfiguration.ALL_DISCOVERY_STRATEGIES )
                     .split( "\\s*,\\s*" );

                config.setDiscoveryStrategies( strat );

                if ( isNotBlank( user ) && isNotBlank( pass ) )
                {
                    config.setRouterCredentials( user, pass );
                }
            }
            catch ( final IOException e )
            {
                throw new MirrorRouterConfigurationException( "Failed to read mirror-router config properties from: '"
                                + source.getSource() + "'.", e );
            }
            finally
            {
                close( stream );
            }
        }
        else
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Cannot read mirror-router configuration from: " + source.getSource()
                                + ". Using defaults." );
            }
        }

        return config;
    }

}
