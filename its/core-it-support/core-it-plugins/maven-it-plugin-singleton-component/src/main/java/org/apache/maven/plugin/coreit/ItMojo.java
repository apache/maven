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
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Requires a singleton component in various ways and dumps the ids to a properties file.
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
     * Component lookup without role hint.
     *
     * @component
     */
    private Component componentWithoutRoleHint;

    /**
     * Component lookup with explicit role hint.
     *
     * @component roleHint="default"
     */
    private Component componentWithRoleHint;

    /**
     * Component lookup via active map.
     *
     * @component role="org.apache.maven.plugin.coreit.Component"
     */
    private Map componentMap;

    /**
     * Component lookup via active list.
     *
     * @component role="org.apache.maven.plugin.coreit.Component"
     */
    private List componentList;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        Component componentFromMap = (Component) componentMap.values().iterator().next();
        Component componentFromList = (Component) componentList.iterator().next();

        getLog().info( "[MAVEN-CORE-IT-LOG] Using component: " + componentWithoutRoleHint );
        getLog().info( "[MAVEN-CORE-IT-LOG] Using component: " + componentWithRoleHint );
        getLog().info( "[MAVEN-CORE-IT-LOG] Using component: " + componentFromMap );
        getLog().info( "[MAVEN-CORE-IT-LOG] Using component: " + componentFromList );

        Properties props = new Properties();
        props.setProperty( "id.0", componentWithoutRoleHint.getId() );
        props.setProperty( "id.1", componentWithRoleHint.getId() );
        props.setProperty( "id.2", componentFromMap.getId() );
        props.setProperty( "id.3", componentFromList.getId() );

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
