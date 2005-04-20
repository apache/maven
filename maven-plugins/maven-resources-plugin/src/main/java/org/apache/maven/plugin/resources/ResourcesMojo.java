package org.apache.maven.plugin.resources;

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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal resources
 * @description copy application resources
 * @parameter name="outputDirectory"
 * type="String"
 * required="true"
 * validator=""
 * expression="#project.build.outputDirectory"
 * description=""
 * @parameter name="resources"
 * type="List"
 * required="true"
 * validator=""
 * expression="#project.build.resources"
 * description=""
 */
public class ResourcesMojo
    extends AbstractPlugin
{
    private String outputDirectory;

    private List resources;

    public void execute()
        throws PluginExecutionException
    {
        try
        {
            for ( Iterator i = getJarResources( resources ).entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                String source = (String) entry.getKey();
                String destination = (String) entry.getValue();
                
                File destinationFile = new File( outputDirectory, destination );

                if ( !destinationFile.getParentFile().exists() )
                {
                    destinationFile.getParentFile().mkdirs();
                }

                fileCopy( source, destinationFile.getPath() );
            }
        }
        catch ( Exception e )
        {
            // TODO: handle exception
            throw new PluginExecutionException( "Error copying resources", e );
        }
    }

    private Map getJarResources( List resources )
        throws Exception
    {
        Map resourceEntries = new TreeMap();

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

            String includesAsString = "**/**";

            java.util.List includes = resource.getIncludes();
            if ( includes != null && includes.size() > 0 )
            {
                includesAsString = StringUtils.join( includes.iterator(), "," );
            }

            List excludes = resource.getExcludes();

            if ( excludes == null )
            {
                excludes = resource.getDefaultExcludes();
            }
            else
            {
                excludes = new ArrayList( excludes );
                excludes.addAll( resource.getDefaultExcludes() );
            }

            String excludesAsString = StringUtils.join( excludes.iterator(), "," );

            List files = FileUtils.getFileNames( resourceDirectory, includesAsString, excludesAsString, false );

            for ( Iterator j = files.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                String entryName = name;

                if ( targetPath != null )
                {
                    entryName = targetPath + "/" + name;
                }

                String resourcePath = new File( resource.getDirectory(), name ).getPath();

                resourceEntries.put( resourcePath, entryName );
            }
        }

        return resourceEntries;
    }

    public static byte[] fileRead( String fileName )
        throws IOException
    {
        FileInputStream in = new FileInputStream( fileName );
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int count;
        byte[] b = new byte[512];
        while ( ( count = in.read( b ) ) > 0 )  // blocking read
        {
            buffer.write( b, 0, count );
        }

        in.close();

        byte[] content = buffer.toByteArray();

        buffer.close();

        return content;
    }

    public static void fileWrite( String fileName, byte[] data )
        throws Exception
    {
        FileOutputStream out = new FileOutputStream( fileName );
        out.write( data );
        out.close();
    }

    public static void fileCopy( String inFileName, String outFileName )
        throws Exception
    {
        byte[] content = fileRead( inFileName );
        fileWrite( outFileName, content );
    }

}
