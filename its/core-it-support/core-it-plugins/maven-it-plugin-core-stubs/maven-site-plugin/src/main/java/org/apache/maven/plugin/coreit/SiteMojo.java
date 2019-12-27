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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

/**
 * Creates a text file in the project base directory.
 * 
 * @goal site
 * @phase site
 * 
 * @author Benjamin Bentmann
 *
 */
public class SiteMojo
    extends AbstractMojo
{

    /**
     * The current Maven project.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The path to the output file, relative to the project base directory directory.
     * 
     * @parameter
     */
    private String pathname = "target/site-site.txt";

    /**
     * Runs this mojo.
     * 
     * @throws MojoExecutionException If the output file could not be created.
     * @throws MojoFailureException If the output file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using output file path: " + pathname );

        if ( pathname == null || pathname.length() <= 0 )
        {
            throw new MojoFailureException( "Path name for output file has not been specified" );
        }

        File outputFile = new File( pathname );
        if ( !outputFile.isAbsolute() )
        {
            outputFile = new File( project.getBasedir(), pathname ).getAbsoluteFile();
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + outputFile );

        try
        {
             outputFile.getParentFile().mkdirs();
             outputFile.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + pathname, e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + outputFile );
    }

}
