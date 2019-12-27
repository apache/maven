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
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Dumps this mojo's configuration into a properties file.
 * 
 * @goal cli-config
 * @phase validate
 * 
 * @author Benjamin Bentmann
 *
 */
public class CliConfigMojo
    extends AbstractMojo
{

    /**
     * The current project's base directory, used for path alignment.
     * 
     * @parameter default-value="${basedir}"
     * @readonly
     */
    private File basedir;

    /**
     * The path to the properties file into which to save the mojo configuration.
     * 
     * @parameter property="config.propertiesFile"
     */
    private File propertiesFile;

    /**
     * A simple parameter of type {@link java.lang.String}.
     * 
     * @parameter property="config.stringParam"
     */
    private String stringParam;

    /**
     * A simple parameter of type {@link java.io.File}.
     * 
     * @parameter property="config.fileParam"
     */
    private File fileParam;

    /**
     * An array parameter of component type {@link java.lang.String}.
     * 
     * @parameter property="config.stringParams"
     */
    private String[] stringParams;

    /**
     * An array parameter of component type {@link java.io.File}.
     * 
     * @parameter property="config.fileParams"
     */
    private File[] fileParams;

    /**
     * A collection parameter of type {@link java.util.List}.
     * 
     * @parameter property="config.listParam"
     */
    private List listParam;

    /**
     * A collection parameter of type {@link java.util.Set}.
     * 
     * @parameter property="config.setParam"
     */
    private Set setParam;

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
        /*
         * NOTE: This intentionally does not dump the absolute path of a file to check the actual value that was
         * injected by Maven.
         */
        PropertiesUtil.serialize( props, "propertiesFile", propertiesFile );
        PropertiesUtil.serialize( props, "stringParam", stringParam );
        PropertiesUtil.serialize( props, "fileParam", fileParam );
        PropertiesUtil.serialize( props, "stringParams", stringParams );
        PropertiesUtil.serialize( props, "fileParams", fileParams );
        PropertiesUtil.serialize( props, "listParam", listParam );
        PropertiesUtil.serialize( props, "setParam", setParam );
    }

}
