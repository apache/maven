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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.codehaus.plexus.logging.Logger;

public class FileRouterConfigLoader
    implements ArtifactRouterConfigLoader
{

    private static final String KEY_ROUTES_FILE = "routing-tables-file";
    
    private static final String KEY_MIRROR_SOURCE = "mirror-source";

    private static final String KEY_GROUP_SOURCE = "group-source";

    private static final String KEY_DISABLED = "disabled";

    private static final String KEY_DISCOVERY_STRATEGY = "discovery-strategy";

    private static final String KEY_SELECTION_STRATEGY = "selection-strategy";
    
    private static final String ROUTES_FILE = "artifact-routes.json";

    private static final String CONFIG_FILENAME = "router.properties";

    public static final String DEFAULT_SOURCE_ID = "central.router";

    private Logger logger;

    private File confDir;

    public FileRouterConfigLoader( File confDir, Logger logger )
    {
        this.confDir = confDir;
        this.logger = logger;
    }

    public ArtifactRouterConfiguration build()
        throws ArtifactRouterConfigurationException
    {
        RouterConfigBuilder builder = new RouterConfigBuilder();
        File routerConfig = new File( confDir, CONFIG_FILENAME );

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Loading mirror configuration from file: " + routerConfig );
        }

        File routesFile = null;
        
        if ( routerConfig.canRead() )
        {
            InputStream stream = null;
            try
            {
                stream = new FileInputStream( routerConfig );
                final Properties p = new Properties();
                p.load( stream );

                String path = p.getProperty( KEY_ROUTES_FILE );
                if ( path != null )
                {
                    routesFile = new File( path );
                }

                builder.withEnabled( !Boolean.parseBoolean( p.getProperty( KEY_DISABLED, "false" ) ) );

                String strat = p.getProperty( KEY_DISCOVERY_STRATEGY );
                if ( strat != null )
                {
                    builder.withDiscoveryStrategy( strat );
                }

                strat = p.getProperty( KEY_SELECTION_STRATEGY );
                if ( strat != null )
                {
                    builder.withDiscoveryStrategy( strat );
                }

                RouterSource src = loadRouterSource( p, KEY_MIRROR_SOURCE );
                if ( src != null )
                {
                    builder.withMirrorSource( src );
                }
                
                src = loadRouterSource( p, KEY_GROUP_SOURCE );
                if ( src != null )
                {
                    builder.withGroupSource( src );
                }
            }
            catch ( final IOException e )
            {
                throw new ArtifactRouterConfigurationException( "Failed to read router config properties from: '"
                    + routerConfig + "'.\nReason: " + e.getMessage(), e );
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
        
        if ( routesFile == null )
        {
            routesFile = new File( confDir, ROUTES_FILE );
        }
        
        builder.withRoutesFile( routesFile );
        
        return builder.build();
    }

    private RouterSource loadRouterSource( Properties p, String keyMirrorSource )
        throws MalformedURLException
    {
        String url = p.getProperty( KEY_MIRROR_SOURCE );
        String id = DEFAULT_SOURCE_ID;
        
        if ( url != null )
        {
            URL u = new URL( url );
            if ( u.getUserInfo() != null )
            {
                id = u.getUserInfo();
                
                StringBuilder sb = new StringBuilder( u.getProtocol() ).append( "://" ).append( u.getHost() );
                if ( u.getPort() > 0 )
                {
                    sb.append( ":" ).append( u.getPort() );
                }

                if ( u.getFile() != null )
                {
                    sb.append( u.getFile() );
                }

                url = sb.toString();
            }
        }
        
        return new RouterSource( id, url );
    }

}
