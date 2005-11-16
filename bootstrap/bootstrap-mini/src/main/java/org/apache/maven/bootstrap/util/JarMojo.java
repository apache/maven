package org.apache.maven.bootstrap.util;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarMojo
{
    private byte[] buffer = new byte[4096];

    private static final String MF = "META-INF/MANIFEST.MF";

    public void execute( File basedir, File jarFile )
        throws Exception
    {
        Map includes = new LinkedHashMap();

        addDirectory( includes, "**/**", "**/package.html,**/.svn/**", "", basedir );

        createJar( jarFile, includes );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes a map <String, File> of items to be include in the outpur
     * @param baseDir the directory to add
     */
    protected void addDirectory( Map includes, File baseDir )
        throws IOException
    {
        addDirectory( includes, "", baseDir );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes a map <String, File> of items to be include in the outpur
     * @param prefix value to be added to the front of jar entry names
     * @param baseDir the directory to add
     */
    protected void addDirectory( Map includes, String prefix, File baseDir )
        throws IOException
    {
        addDirectory( includes, null, null, prefix, baseDir );
    }

    /**
     * Add all files in the specified directory to the archive.
     *
     * @param includes a map <String, File> of items to be include in the outpur
     * @param includesPattern Sets the list of include patterns to use
     * @param excludesPattern Sets the list of exclude patterns to use
     * @param prefix value to be added to the front of jar entry names
     * @param baseDir the directory to add
     */
    protected void addDirectory( Map includes, String includesPattern, String excludesPattern, String prefix,
                                 File baseDir )
        throws IOException
    {
        if ( !baseDir.exists() )
        {
            return;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( baseDir );
        if ( includesPattern != null )
        {
            scanner.setIncludes( StringUtils.split( includesPattern, "," ) );
        }

        if ( excludesPattern != null )
        {
            scanner.setExcludes( StringUtils.split( excludesPattern, "," ) );
        }
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            String file = files[i];
            file = file.replace( '\\', '/' ); // todo shouldn't the scanner return platform independent names?
            includes.put( prefix + file, new File( baseDir, file ) );
        }
    }

    /**
     * Create a manifest for the jar file
     *
     * @return a default manifest; the Manifest-Version and Created-By attributes are initialized
     */
    protected Manifest createManifest()
    {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue( Attributes.Name.MANIFEST_VERSION.toString(), "1.0" );
        attrs.putValue( "Created-By", "Apache Maven Bootstrap Mini" );
        return mf;
    }

    /**
     * Create the jar file specified and include the listed files.
     *
     * @param jarFile the jar file to create
     * @param includes a Map<String, File>of items to include; the key is the jar entry name
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void createJar( File jarFile, Map includes )
        throws IOException
    {
        jarFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream( jarFile );
        JarOutputStream jos = null;
        try
        {
            if ( includes.containsKey( MF ) )
            {
                jos = new JarOutputStream( fos );
            }
            else
            {
                jos = new JarOutputStream( fos, createManifest() );
            }
            addEntries( jos, includes );
        }
        finally
        {
            IOUtil.close( jos );
            IOUtil.close( fos );
        }
    }

    /**
     * Add all entries in the supplied Map to the jar
     *
     * @param jos a JarOutputStream that can be used to write to the jar
     * @param includes a Map<String, File> of entries to add
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void addEntries( JarOutputStream jos, Map includes )
        throws IOException
    {
        for ( Iterator i = includes.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            File file = (File) entry.getValue();
            addEntry( jos, name, file );
        }
    }

    /**
     * Add a single entry to the jar
     *
     * @param jos a JarOutputStream that can be used to write to the jar
     * @param name the entry name to use; must be '/' delimited
     * @param source the file to add
     * @throws IOException if there is a problem writing the archive or reading the sources
     */
    protected void addEntry( JarOutputStream jos, String name, File source )
        throws IOException
    {
        FileInputStream fis = new FileInputStream( source );
        try
        {
            jos.putNextEntry( new JarEntry( name ) );
            int count;
            while ( ( count = fis.read( buffer ) ) > 0 )
            {
                jos.write( buffer, 0, count );
            }
            jos.closeEntry();
        }
        finally
        {
            fis.close();
        }
    }

}
