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

import java.io.File;
import java.io.IOException;

import org.apache.maven.coreit.component.StatefulSingleton;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Updates the state of the singleton component and optionally dumps the updated state to a properties file.
 *
 * @goal update-singleton
 * @phase initialize
 *
 * @author Benjamin Bentmann
 */
public class UpdateSingletonMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="provider"
     */
    private String key;

    /**
     * @parameter default-value="passed"
     */
    private String value;

    /**
     * @parameter
     */
    private File propertiesFile;

    /**
     * @component
     */
    private StatefulSingleton singleton;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Singleton Instance: " + System.identityHashCode( singleton ) );
        getLog().info( "[MAVEN-CORE-IT-LOG] Singleton Class Loader: " + singleton.getClass().getClassLoader() );

        getLog().info( "[MAVEN-CORE-IT-LOG] Setting property " + key + " = " + value );

        singleton.setProperty( key, value );

        if ( propertiesFile != null )
        {
            getLog().info( "[MAVEN-CORE-IT-LOG] Saving properties to " + propertiesFile );

            try
            {
                singleton.saveProperties( propertiesFile );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to save properties to " + propertiesFile, e );
            }
        }
    }

}
