package org.apache.maven.artifact.router.conf;

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
import static org.codehaus.plexus.util.StringUtils.isNotBlank;

import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FileRouterConfigBuilder
    implements RouterConfigBuilder
{

    private static final String KEY_ROUTER_MIRRORS_URL = "router-mirrors-url";

    private static final String KEY_ROUTER_GROUPS_URL = "router-groups-url";
    
    private static final String KEY_ROUTES_FILE = "routing-tables-file";

    private static final String KEY_ROUTER_USER = "router-user";

    private static final String KEY_ROUTER_PASSWORD = "router-password";

    private static final String KEY_DISABLED = "disabled";

    private static final String KEY_DISCOVERY_STRATEGIES = "discovery-strategies";

    private static final String ROUTES_FILE = "artifact-routes.json";
    
    private static final String CONFIG_FILENAME = "router.properties";
    
    private Logger logger;
    
    private File confDir;
    
    public FileRouterConfigBuilder( File confDir, Logger logger )
    {
        this.confDir = confDir;
        this.logger = logger;
    }

    public ArtifactRouterConfiguration build()
        throws ArtifactRouterConfigurationException
    {
        final ArtifactRouterConfiguration config = new ArtifactRouterConfiguration();
        File routerConfig = new File( confDir, CONFIG_FILENAME );

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Loading mirror configuration from file: " + routerConfig );
        }

        if ( routerConfig.canRead() )
        {
            InputStream stream = null;
            try
            {
                stream = new FileInputStream( routerConfig );
                final Properties p = new Properties();
                p.load( stream );

                config.setRouterMirrorsUrl( p.getProperty( KEY_ROUTER_MIRRORS_URL ) );
                config.setRouterGroupsUrl( p.getProperty( KEY_ROUTER_GROUPS_URL ) );
                
                String path = p.getProperty( KEY_ROUTES_FILE );
                if ( path != null )
                {
                    config.setRoutesFile( new File( path ) );
                }

                config.setDisabled( Boolean.parseBoolean( p.getProperty( KEY_DISABLED, "false" ) ) );

                final String user = p.getProperty( KEY_ROUTER_USER );
                final String pass = p.getProperty( KEY_ROUTER_PASSWORD );

                final String[] strat =
                    p.getProperty( KEY_DISCOVERY_STRATEGIES, ArtifactRouterConfiguration.ALL_DISCOVERY_STRATEGIES )
                     .split( "\\s*,\\s*" );

                config.setDiscoveryStrategies( strat );

                if ( isNotBlank( user ) && isNotBlank( pass ) )
                {
                    config.setRouterCredentials( user, pass );
                }
            }
            catch ( final IOException e )
            {
                throw new ArtifactRouterConfigurationException( "Failed to read router config properties from: '"
                                + routerConfig + "'.", e );
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
                logger.debug( "Cannot read router configuration from: " + routerConfig
                                + ". Using defaults." );
            }
        }
        
        if ( config.getRoutesFile() == null )
        {
            config.setRoutesFile( new File( confDir, ROUTES_FILE ) );
        }

        return config;
    }

}
