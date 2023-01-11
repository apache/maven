package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

/**
 * Provides common services for all mojos of this plugin.
 *
 * @author Benjamin Bentmann
 *
 */
public abstract class AbstractDependencyMojo
    extends AbstractMojo
{

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    protected MavenProject project;

    /**
     * Writes the specified artifacts to the given output file.
     *
     * @param pathname  The path to the output file, relative to the project base directory, may be <code>null</code> or
     *                  empty if the output file should not be written.
     * @param artifacts The list of artifacts to write to the file, may be <code>null</code>.
     * @throws MojoExecutionException If the output file could not be written.
     */
    protected void writeArtifacts( String pathname, Collection artifacts )
        throws MojoExecutionException
    {
        if ( pathname == null || pathname.length() <= 0 )
        {
            return;
        }

        File file = resolveFile( pathname );

        getLog().info( "[MAVEN-CORE-IT-LOG] Dumping artifact list: " + file );

        BufferedWriter writer = null;
        try
        {
            file.getParentFile().mkdirs();

            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ) );

            if ( artifacts != null )
            {
                for ( Object artifact1 : artifacts )
                {
                    Artifact artifact = (Artifact) artifact1;
                    writer.write( artifact.getId() );
                    String optional = "";
                    if ( artifact.isOptional() )
                    {
                        optional = " (optional)";
                        writer.write( optional );
                    }
                    writer.newLine();
                    getLog().info( "[MAVEN-CORE-IT-LOG]   " + artifact.getId() + optional );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write artifact list", e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }
    }

    // NOTE: We don't want to test path translation here so resolve relative path manually for robustness
    private File resolveFile( String pathname )
    {
        File file = null;

        if ( pathname != null )
        {
            file = new File( pathname );

            if ( !file.isAbsolute() )
            {
                file = new File( project.getBasedir(), pathname );
            }
        }

        return file;
    }

}
