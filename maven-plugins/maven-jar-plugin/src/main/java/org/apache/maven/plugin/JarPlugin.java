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

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 *
 * maven.jar.manifest.extensions.add
 * maven.jar.includes
 * maven.jar.excludes
 * maven.jar.index
 * maven.jar.compress
 * maven.remote.group
 */
public class JarPlugin
{
    private String jarName;

    private String outputDirectory;

    private String basedir;

    public void execute()
        throws Exception
    {
        File basedir = new File( this.basedir );

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
