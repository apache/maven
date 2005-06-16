package org.apache.maven.plugin.registry;

import org.apache.maven.plugin.registry.io.xpp3.PluginRegistryXpp3Reader;
import org.apache.maven.plugin.registry.TrackableBase;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DefaultPluginRegistryBuilder
    extends AbstractLogEnabled
    implements MavenPluginRegistryBuilder, Initializable
{

    public static final String userHome = System.getProperty( "user.home" );

    /**
     * @configuration
     */
    private String userRegistryPath;

    /**
     * @configuration
     */
    private String globalRegistryPath;

    private File userRegistryFile;

    private File globalRegistryFile;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
    {
        userRegistryFile = getFile( userRegistryPath, "user.home", MavenPluginRegistryBuilder.ALT_USER_PLUGIN_REG_LOCATION );
        
        globalRegistryFile = getFile( globalRegistryPath, "maven.home", MavenPluginRegistryBuilder.ALT_GLOBAL_PLUGIN_REG_LOCATION );

        getLogger().debug( "Building Maven global-level settings from: '" + globalRegistryFile.getAbsolutePath() + "'" );
        getLogger().debug( "Building Maven user-level settings from: '" + userRegistryFile.getAbsolutePath() + "'" );
    }
    
    public PluginRegistry buildPluginRegistry()
        throws IOException, XmlPullParserException
    {
        PluginRegistry global = readPluginRegistry( globalRegistryFile );
        
        PluginRegistry user = readPluginRegistry( userRegistryFile );

        if ( user == null && global != null )
        {
            // we'll use the globals, but first we have to recursively mark them as global...
            PluginRegistryUtils.recursivelySetSourceLevel( global, PluginRegistry.GLOBAL_LEVEL );
            
            user = global;
        }
        else
        {
            // merge non-colliding plugins into the user registry.
            PluginRegistryUtils.merge( user, global, TrackableBase.GLOBAL_LEVEL );
        }

        return user;
    }

    private PluginRegistry readPluginRegistry( File registryFile )
        throws IOException, XmlPullParserException
    {
        PluginRegistry registry = null;

        if ( registryFile.exists() && registryFile.isFile() )
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader( registryFile );

                PluginRegistryXpp3Reader modelReader = new PluginRegistryXpp3Reader();

                registry = modelReader.read( reader );
                
                RuntimeInfo rtInfo = new RuntimeInfo( registry );
                
                registry.setRuntimeInfo( rtInfo );
                
                rtInfo.setFile( registryFile );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        return registry;
    }

    private File getFile( String pathPattern, String basedirSysProp, String altLocationSysProp )
    {
        // -------------------------------------------------------------------------------------
        // Alright, here's the justification for all the regexp wizardry below...
        //
        // Continuum and other server-like apps may need to locate the user-level and 
        // global-level settings somewhere other than ${user.home} and ${maven.home},
        // respectively. Using a simple replacement of these patterns will allow them
        // to specify the absolute path to these files in a customized components.xml
        // file. Ideally, we'd do full pattern-evaluation against the sysprops, but this
        // is a first step. There are several replacements below, in order to normalize
        // the path character before we operate on the string as a regex input, and 
        // in order to avoid surprises with the File construction...
        // -------------------------------------------------------------------------------------
        
        String path = System.getProperty( altLocationSysProp );

        if ( StringUtils.isEmpty( path ) )
        {
            // TODO: This replacing shouldn't be necessary as user.home should be in the
            // context of the container and thus the value would be interpolated by Plexus
            String basedir = System.getProperty( basedirSysProp );

            basedir = basedir.replaceAll( "\\\\", "/" );
            basedir = basedir.replaceAll("\\$", "\\\\\\$");
            
            path = pathPattern.replaceAll( "\\$\\{" + basedirSysProp + "\\}", basedir );
            path = path.replaceAll( "\\\\", "/" );
            path = path.replaceAll( "//", "/" );

            return new File( path ).getAbsoluteFile();
        }
        else
        {
            return new File( path ).getAbsoluteFile();
        }
    }

    public PluginRegistry createUserPluginRegistry()
    {
        PluginRegistry registry = new PluginRegistry();
        
        RuntimeInfo rtInfo = new RuntimeInfo( registry );
        
        registry.setRuntimeInfo( rtInfo );
        
        rtInfo.setFile( userRegistryFile );
        
        return registry;
    }
    
}
