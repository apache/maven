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
 * Check that we correctly use the implementation parameter.
 *
 * @goal param-implementation
 */
public class ParameterImplementationMojo
    extends AbstractMojo
{

    /**
     * The path to the properties file for the parameter information.
     *
     * @parameter
     */
    private File outputFile;

    /**
     * A parameter whose type is an interface but with a default implementation class.
     *
     * @parameter implementation="org.apache.maven.plugin.coreit.sub.AnImplementation"
     */
    private AnInterface theParameter;

    public void execute()
        throws MojoExecutionException
    {
        Properties props = new Properties();

        if ( theParameter != null )
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] theParameter = " + theParameter );

            props.setProperty( "theParameter.class", theParameter.getClass().getName() );
            props.setProperty( "theParameter.string", theParameter.toString() );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file " + outputFile );

        try
        {
            outputFile.getParentFile().mkdirs();

            try ( FileOutputStream os = new FileOutputStream( outputFile ) )
            {
                props.store( os, "[MAVEN-CORE-IT-LOG]" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to create output file " + outputFile + ": " + e.getMessage(), e );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file " + outputFile );
    }

}
