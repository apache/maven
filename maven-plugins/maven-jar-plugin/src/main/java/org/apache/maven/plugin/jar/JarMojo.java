package org.apache.maven.plugin.jar;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.codehaus.plexus.util.FileUtils;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @goal jar
 *
 * @description build a jar
 *
 * @prereq surefire:test
 * @prereq resources:resources
 *
 * @parameter
 *  name="jarName"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#maven.final.name"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * @parameter
 *  name="basedir"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.output"
 *  description=""
 *
 * @author <a href="michal@codehaus">Michal Maczka</a>
 * @version $Id$
 */
public class JarMojo
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File basedir = new File( (String) request.getParameter( "basedir" ) );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String jarName = (String) request.getParameter( "jarName" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------


        File jarFile = new File( new File( outputDirectory ), jarName + ".jar" );

        List files = FileUtils.getFileNames( basedir, "**/**", "**/package.html", false );

        createJar( files, jarFile, basedir );
    }

    public void createJar( List files, File jarName, File basedir )
        throws Exception
    {
        JarOutputStream jar = new JarOutputStream( new FileOutputStream( jarName ), createManifest() );

        try
        {
            for ( int i = 0; i < files.size(); i++ )
            {
                String file = (String) files.get( i );

                writeJarEntry( jar, new File( basedir, file ), file );
            }
        }
        finally
        {
            jar.close();
        }
    }

    private void writeJarEntry( JarOutputStream jar, File source, String entryName )
        throws Exception
    {
        byte[] buffer = new byte[1024];

        int bytesRead;

        try
        {
            FileInputStream is = new FileInputStream( source );

            try
            {
                JarEntry entry = new JarEntry( entryName );

                jar.putNextEntry( entry );

                while ( ( bytesRead = is.read( buffer ) ) != -1 )
                {
                    jar.write( buffer, 0, bytesRead );
                }
            }
            catch ( Exception ex )
            {
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException ex )
        {
        }
    }

    private Manifest createManifest()
    {
        Manifest manifest = new Manifest();

        return manifest;
    }
}
