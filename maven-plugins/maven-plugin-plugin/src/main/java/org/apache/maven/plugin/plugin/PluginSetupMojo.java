package org.apache.maven.plugin.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import org.codehaus.plexus.util.FileUtils;

/*
 * LICENSE
 */

/**
 * @goal setup
 *
 * @description Installs a plugin into the maven installation.
 *
 * @prereq plugin:plugin
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * 
 * @parameter
 *  name="jarName"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#maven.final.name"
 *  description=""
 *
 * @parameter
 *  name="pluginHome"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#maven.plugin.home"
 *  description=""
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class PluginSetupMojo
    extends AbstractPluginMojo
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {


        String pluginHomeName = (String) request.getParameter( "pluginHome" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File jarFile = getJarFile( request );

        File pluginHome = getPluginHome( pluginHomeName );

        System.out.println( "Installing " + jarFile + " in " + pluginHome );

        FileUtils.copyFileToDirectory( jarFile, pluginHome );
    }

    private File getPluginHome( String pluginHomeName )
            throws Exception
    {
        File pluginHome;

        if ( pluginHomeName == null ||
             pluginHomeName.trim().length() == 0 ||
             pluginHomeName.equals( "maven.plugin.home" ) )
        {
            String mavenHomeName = System.getProperty( "maven.home" );

            if ( mavenHomeName == null )
            {
                String userHomeName = System.getProperty( "user.home" );

                System.out.println( "userHomeName: " + userHomeName );

                File mavenHome = new File( userHomeName, ".m2" );

                if ( !mavenHome.exists() )
                {
                    mavenHome = new File( userHomeName, "m2" );

                    if ( !mavenHome.exists() )
                    {
                        pluginHome = new File( mavenHome, "plugins" );
                    }
                    else
                    {
                        throw new Exception( "Cannot find the maven plugins directory." );
                    }
                }
                else
                {
                    pluginHome = new File( mavenHome, "plugins" );
                }
            }
            else
            {
                pluginHome = new File( mavenHomeName, "plugins" );
            }
        }
        else
        {
            pluginHome = new File( pluginHomeName );
        }
        return pluginHome;
    }
}
