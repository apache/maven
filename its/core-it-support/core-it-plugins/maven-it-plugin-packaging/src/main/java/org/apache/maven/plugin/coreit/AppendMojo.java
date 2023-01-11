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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Appends a message to a UTF-8 encoded plain text file.
 *
 * @author Benjamin Bentmann
  */
@Mojo( name = "append" )
public class AppendMojo
    extends AbstractMojo
{

    /**
     */
    @Parameter( property = "append.message" )
    private String message;

    /**
     */
    @Parameter( defaultValue = "${project.build.directory}/log.txt" )
    private File outputFile;

    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + outputFile );

        try
        {
            outputFile.getParentFile().mkdirs();

            getLog().info( "[MAVEN-CORE-IT-LOG]   " + message );

            try ( OutputStreamWriter writer = new OutputStreamWriter( new FileOutputStream( outputFile, true ),
                                                                      "UTF-8" ) )
            {
                writer.write( message );
                writer.write( "\n" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + outputFile, e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + outputFile );
    }

}
