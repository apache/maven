package org.apache.maven.plugin.resources;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @goal resources
 *
 * @description copy application resources
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory/classes"
 *  description=""
 * @parameter
 *  name="resources"
 *  type="List"
 *  required="true"
 *  validator="#project.build.resources"
 *  expression="#project.build.unitTest.resources"
 *  description=""
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 *
 */
public class ResourcesMojo
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        List resources = (List) request.getParameter( "resources" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        for ( Iterator i = getJarResources( resources ).iterator(); i.hasNext(); )
        {
            ResourceEntry resourceEntry = (ResourceEntry) i.next();

            File destinationFile = new File( outputDirectory, resourceEntry.getDestination() );

            if ( !destinationFile.getParentFile().exists() )
            {
                destinationFile.getParentFile().mkdirs();
            }

            fileCopy( resourceEntry.getSource(), destinationFile.getPath() );
        }
    }

    private List getJarResources( List resources )
        throws Exception
    {
        List resourceEntries = new ArrayList();

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            String targetPath = resource.getTargetPath();

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() )
            {
                continue;
            }

            // If we only have a directory then we want to include
            // everything we can find within that path.

            String includes;

            if ( resource.getIncludes().size() > 0 )
            {
                includes = listToString( resource.getIncludes() );
            }
            else
            {
                includes = "**/**";
            }

            List files = FileUtils.getFileNames( resourceDirectory,
                                                 includes,
                                                 listToString( resource.getExcludes() ),
                                                 false );

            for ( Iterator j = files.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                String entryName = name;

                if ( targetPath != null )
                {
                    entryName = targetPath + "/" + name;
                }

                ResourceEntry je = new ResourceEntry( new File( resource.getDirectory(), name ).getPath(), entryName );

                resourceEntries.add( je );
            }
        }

        return resourceEntries;
    }

    private String listToString( List list )
    {
        StringBuffer sb = new StringBuffer();

        for ( int i = 0; i < list.size(); i++ )
        {
            sb.append( list.get( i ) );

            if ( i != list.size() - 1 )
            {
                sb.append( "," );
            }
        }

        return sb.toString();
    }

    public static String fileRead( String fileName ) throws IOException
    {
        StringBuffer buf = new StringBuffer();

        FileInputStream in = new FileInputStream( fileName );

        int count;
        byte[] b = new byte[512];
        while ( ( count = in.read( b ) ) > 0 )  // blocking read
        {
            buf.append( new String( b, 0, count ) );
        }

        in.close();

        return buf.toString();
    }

    public static void fileWrite( String fileName, String data ) throws Exception
    {
        FileOutputStream out = new FileOutputStream( fileName );
        out.write( data.getBytes() );
        out.close();
    }

    public static void fileCopy( String inFileName, String outFileName ) throws
        Exception
    {
        String content = fileRead( inFileName );
        fileWrite( outFileName, content );
    }

    class ResourceEntry
    {
        private String source;

        private String destination;

        public ResourceEntry( String source, String entry )
        {
            this.source = source;

            this.destination = entry;
        }

        public String getSource()
        {
            return source;
        }

        public String getDestination()
        {
            return destination;
        }
    }
}
