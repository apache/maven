package org.apache.maven.plugin.assemble;

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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugins.assemble.model.Assembly;
import org.apache.maven.plugins.assemble.model.FileSet;
import org.apache.maven.plugins.assemble.model.io.xpp3.AssemblyXpp3Reader;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal assemble
 * @description assemble an application bundle or distribution
 * @parameter name="outputDirectory" type="java.io.File" required="true" validator="" expression="#project.build.directory" description=""
 * @parameter name="descriptor" type="java.io.File" required="true" validator="" expression="#maven.assemble.descriptor" description=""
 * @parameter name="finalName" type="String" required="true" validator="" expression="#project.build.finalName" description=""
 */
public class AssembleMojo
    extends AbstractPlugin
{
    private static final String[] EMPTY_STRING_ARRAY = {};

    private File outputDirectory;

    private File descriptor;

    private String finalName;

    public void execute()
        throws PluginExecutionException
    {
        try
        {
            doExecute();
        }
        catch ( Exception e )
        {
            // TODO: don't catch exception
            throw new PluginExecutionException( "Error creating assembly", e );
        }
    }

    private void doExecute()
        throws Exception
    {
        AssemblyXpp3Reader reader = new AssemblyXpp3Reader();
        Assembly assembly = reader.read( new FileReader( descriptor ) );

        // TODO: include dependencies marked for distribution under certain formats
        // TODO: have a default set of descriptors that can be used instead of the file
        // TODO: how, might we plugin this into an installer, such as NSIS?
        // TODO: allow file mode specifications?

        String fullName = finalName + "-" + assembly.getId();

        for ( Iterator i = assembly.getFormats().iterator(); i.hasNext(); )
        {
            String format = (String) i.next();

            String filename = fullName + "." + format;

            // TODO: use component roles? Can we do that in a mojo?
            Archiver archiver;
            if ( format.startsWith( "tar" ) )
            {
                TarArchiver tarArchiver = new TarArchiver();
                archiver = tarArchiver;
                int index = format.indexOf( '.' );
                if ( index >= 0 )
                {
                    // TODO: this needs a cleanup in plexus archiver - use a real typesafe enum
                    TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                    // TODO: this should accept gz and bz2 as well so we can skip over the switch
                    String compression = format.substring( index + 1 );
                    if ( compression.equals( "gz" ) )
                    {
                        tarCompressionMethod.setValue( "gzip" );
                    }
                    else if ( compression.equals( "bz2" ) )
                    {
                        tarCompressionMethod.setValue( "bzip2" );
                    }
                    else
                    {
                        // TODO: better handling
                        throw new IllegalArgumentException( "Unknown compression format: " + compression );
                    }
                    tarArchiver.setCompression( tarCompressionMethod );
                }
            }
            else if ( format.startsWith( "zip" ) )
            {
                archiver = new ZipArchiver();
            }
            else if ( format.startsWith( "jar" ) )
            {
                // TODO: use MavenArchiver for manifest?
                archiver = new JarArchiver();
            }
            else
            {
                // TODO: better handling
                throw new IllegalArgumentException( "Unknown format: " + format );
            }

            for ( Iterator j = assembly.getFilesets().iterator(); j.hasNext(); )
            {
                FileSet fileset = (FileSet) j.next();
                String directory = fileset.getDirectory();
                String output = fileset.getOutputDirectory();
                if ( output == null )
                {
                    output = directory;
                }
                if ( !output.endsWith( "/" ) && !output.endsWith( "\\" ) )
                {
                    // TODO: shouldn't archiver do this?
                    output += '/';
                }

                String[] includes = (String[]) fileset.getIncludes().toArray( EMPTY_STRING_ARRAY );
                if ( includes.length == 0 )
                {
                    includes = null;
                }
                String[] excludes = (String[]) fileset.getExcludes().toArray( EMPTY_STRING_ARRAY );
                archiver.addDirectory( new File( directory ), output, includes, excludes );
            }

            archiver.setDestFile( new File( outputDirectory, filename ) );
            archiver.createArchive();
        }
    }
}
