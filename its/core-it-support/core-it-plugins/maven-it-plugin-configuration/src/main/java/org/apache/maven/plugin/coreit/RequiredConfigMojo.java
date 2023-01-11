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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Dumps this mojo's configuration into a properties file.
 *
  *
 * @author Benjamin Bentmann
 */
@Mojo( name = "required-config", defaultPhase = LifecyclePhase.VALIDATE )
public class RequiredConfigMojo
    extends AbstractMojo
{

    /**
     * The current project's base directory, used for path alignment.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true )
    private File basedir;

    /**
     * The path to the properties file into which to save the mojo configuration.
     */
    @Parameter( property = "config.propertiesFile" )
    private File propertiesFile;

    /**
     * A required parameter to be set via plugin configuration in POM or system property from CLI.
     */
    @Parameter( property = "config.requiredParam", required = true )
    private String requiredParam;

    /**
     * A required parameter that defaults to a non-mandatory value from the POM.
     */
    @Parameter( defaultValue = "${project.url}", required = true )
    private String requiredParamWithDefault;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using output file path: " + propertiesFile );

        if ( propertiesFile == null )
        {
            throw new MojoExecutionException( "Path name for output file has not been specified" );
        }

        if ( !propertiesFile.isAbsolute() )
        {
            propertiesFile = new File( basedir, propertiesFile.getPath() ).getAbsoluteFile();
        }

        Properties configProps = new Properties();

        dumpConfiguration( configProps );

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + propertiesFile );

        PropertiesUtil.write( propertiesFile, configProps );

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + propertiesFile );
    }

    /**
     * Dumps the mojo configuration into the specified properties.
     *
     * @param props The properties to dump the configuration into, must not be <code>null</code>.
     */
    private void dumpConfiguration( Properties props )
    {
        PropertiesUtil.serialize( props, "requiredParam", requiredParam );
        PropertiesUtil.serialize( props, "requiredParamWithDefault", requiredParamWithDefault );
    }

}
