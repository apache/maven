package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

//MAPI: This is a canidate for the internal state dump (ISD). This is probably similar to what is in the help plugin.

/**
 * @goal config
 * @phase generate-resources
 * @description Goal produces a raw string with contains full interpolated plugin configurations.
 */
public class CoreItMojo
    extends AbstractMojo
{
    /** @parameter expression="${dom}" */
    private PlexusConfiguration dom;

    /** @parameter expression="${outputDirectory}" default-value="${project.build.directory}" */
    private File outputDirectory;

    /** @parameter expression="${fileName}" default-value="plugin-configuration.txt" */
    private String fileName;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            File file = new File( outputDirectory, fileName );

            if ( !outputDirectory.exists() )
            {
                outputDirectory.mkdirs();
            }

            Writer writer = new FileWriter( file );

            writer.write( dom.toString() );

            writer.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing out plugin configuration.", e );
        }
    }
}
