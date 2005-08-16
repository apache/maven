package org.apache.maven.plugin.assembly;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.IOUtil;

/**
 * Base routines for assembly and unpack goals
 *
 * @version $Id$
 */
public abstract class AbstractUnpackingMojo
    extends AbstractMojo
{
    static protected final String[] EMPTY_STRING_ARRAY = {};

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * @parameter expression="${project.artifacts}"
     * @readonly
     */
    protected Set dependencies;

    /**
	 * Directory to unpack JARs into if needed
	 * @parameter  expression="${project.build.directory}/assembly/work"
	 * @required
	 */
	protected File workDirectory;

	protected void unpack(File file, File location) throws IOException {
		String fileName = file.getAbsolutePath().toLowerCase().trim();
		// Should be checking for '.' too?
		// Not doing this to be consistent with existing code
		if ( fileName.endsWith( "jar" ) )
		{
			unpackJar( file, location );
		}
		else if( fileName.endsWith( "zip" ) )
		{
			unpackZip( file, location );
		}
	}

	private void unpackJar( File file, File tempLocation )
	    throws IOException
	{
	    if ( !file.getAbsolutePath().toLowerCase().trim().endsWith( "jar" ) )
	    {
	        getLog().warn( "Trying to unpack a non-jar file " + file.getAbsolutePath() + " - IGNORING" );
	        return;
	    }
	
	    JarFile jar = new JarFile( file );
	    for ( Enumeration e = jar.entries(); e.hasMoreElements(); )
	    {
	        JarEntry entry = (JarEntry) e.nextElement();
	
	        if ( !entry.isDirectory() )
	        {
	            File outFile = new File( tempLocation, entry.getName() );
	            outFile.getParentFile().mkdirs();
	            IOUtil.copy( jar.getInputStream( entry ), new FileOutputStream( outFile ) );
	        }
	    }
	}

	private void unpackZip(File file, File tempLocation) throws IOException {
	    if ( !file.getAbsolutePath().toLowerCase().trim().endsWith( "zip" ) )
	    {
	        getLog().warn( "Trying to unpack a non-zip file " + file.getAbsolutePath() + " - IGNORING" );
	        return;
	    }
	
	    ZipFile zip = new ZipFile( file );
	    for ( Enumeration e = zip.entries(); e.hasMoreElements(); )
	    {
	        ZipEntry entry = (ZipEntry) e.nextElement();
	
	        if ( !entry.isDirectory() )
	        {
	            File outFile = new File( tempLocation, entry.getName() );
	            outFile.getParentFile().mkdirs();
	            IOUtil.copy( zip.getInputStream( entry ), new FileOutputStream( outFile ) );
	        }
	    }
	}


}
