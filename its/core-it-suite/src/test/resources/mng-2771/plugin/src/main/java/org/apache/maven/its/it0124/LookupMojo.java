package org.apache.maven.its.it0124;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Simple mojo to write the injected artifact factory implementation to a file.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @goal lookup
 */
public class LookupMojo extends AbstractMojo
{
    /**
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${project.build.directory}"
     */
    private String buildDirectory;

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File file = new File( buildDirectory, "lookup.log" );

        if ( !file.getParentFile().mkdirs() )
        {
            throw new MojoExecutionException( "Cannot create build directory" );
        }

        try
        {
            Writer writer = new FileWriter( file );
            writer.write( artifactFactory == null ? "null" : artifactFactory.getClass().getName() );
            writer.close();
        }
        catch ( IOException exception )
        {
            throw new MojoExecutionException( "Cannot create lookup.log", exception );
        }
    }
}
