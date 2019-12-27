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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Requires a component with a non-default role hint and dumps this hint to a properties file.
 *
 * @author Benjamin Bentmann
 *
 * @goal it
 * @phase initialize
 */
public class ItMojo
    extends AbstractMojo
{

    /**
     * The path to the output file.
     *
     * @parameter property="touch.outputFile" default-value="target/comp.properties"
     */
    private File outputFile;

    /**
     * NOTE: We don't specify a role hint here!
     *
     * @component
     */
    private Component component;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using component: " + component );

        Properties props = new Properties();
        props.setProperty( "id", component.getId() );

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + outputFile );

        try
        {
            outputFile.getParentFile().mkdirs();
            try ( FileOutputStream os = new FileOutputStream( outputFile ) )
            {
                props.store( os, "MAVEN-CORE-IT-LOG" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + outputFile, e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + outputFile );
    }

}
