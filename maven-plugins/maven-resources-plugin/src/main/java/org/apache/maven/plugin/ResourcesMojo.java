package org.apache.maven.plugin;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Maven" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Maven", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */

import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @maven.plugin.id resources
 * @maven.plugin.description Maven plugin to build jars
 *
 * @parameter outputDirectory String true validator description
 * @parameter resources List true validator description
 *
 * @goal resources
 * @goal.description copy application resources
 * @goal.parameter outputDirectory #project.build.directory/classes
 * @goal.parameter resources #project.build.resources
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
