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
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo with deprecated params.
 * Dumps this mojo's configuration into a properties file.
 *
 * @author Slawomir Jaranowski
 */
@Mojo( name = "deprecated-config", defaultPhase = LifecyclePhase.VALIDATE )
public class DeprecatedConfigMojo
    extends AbstractMojo
{

    /**
     * The current project's base directory, used for path alignment.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true )
    private File basedir;

    /**
     * @deprecated bean read only
     */
    @Parameter( defaultValue = "${project.artifact}", readonly = true )
    private Artifact deprecatedBeanReadOnly;

    /**
     * @deprecated bean
     */
    @Parameter( defaultValue = "${project.artifact}" )
    private Artifact deprecatedBean;

    /**
     * The path to the properties file into which to save the mojo configuration.
     */
    @Parameter( defaultValue = "${project.build.directory}/config.properties" )
    private File propertiesFile;

    /**
     * A deprecated parameter to be set via plugin configuration in POM.
     *
     * @deprecated I'm deprecated param
     */
    @Deprecated
    @Parameter
    private String deprecatedParam;

    /**
     * A deprecated parameter without message to be set via plugin configuration in POM.
     *
     * @deprecated
     */
    @Deprecated
    @Parameter( property = "config.deprecatedParam2" )
    private String deprecatedParam2;

    /**
     * A deprecated parameter that defaults to a non-mandatory value from the POM.
     *
     * @deprecated deprecated with constant value
     */
    @Deprecated
    @Parameter( defaultValue = "testValue" )
    private String deprecatedParamWithDefaultConstant;

    /**
     * A deprecated parameter that defaults to a non-mandatory value from the POM.
     *
     * @deprecated deprecated with evaluate value
     */
    @Deprecated
    @Parameter( defaultValue = "${project.url}" )
    private String deprecatedParamWithDefaultEvaluate;

    /**
     * @deprecated deprecated array
     */
    @Deprecated
    @Parameter( property = "config.deprecatedArray" )
    private String[] deprecatedArray;

    /**
     * @deprecated deprecated array
     */
    @Deprecated
    @Parameter( defaultValue = "a1 ,a2, a3" )
    private String[] deprecatedArrayWithDefaults;

    /**
     * @deprecated deprecated map
     */
    @Deprecated
    @Parameter
    private Map<String, String> deprecatedMap;

    /**
     * @deprecated deprecated properties
     */
    @Deprecated
    @Parameter
    private Properties deprecatedProperties;

    /**
     * @deprecated deprecated list
     */
    @Deprecated
    @Parameter( property = "config.deprecatedList" )
    private List<String> deprecatedList;

    /**
     * @deprecated deprecated list
     */
    @Deprecated
    @Parameter( defaultValue = "l1,l2,l3" )
    private List<String> deprecatedListWithDefaults;

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
    @SuppressWarnings( "deprecation" )
    private void dumpConfiguration( Properties props )
    {
        PropertiesUtil.serialize( props, "deprecatedParam", deprecatedParam );
        PropertiesUtil.serialize( props, "deprecatedParam2", deprecatedParam2 );
        PropertiesUtil.serialize( props, "deprecatedParamWithDefaultConstant", deprecatedParamWithDefaultConstant );
        PropertiesUtil.serialize( props, "deprecatedParamWithDefaultEvaluate", deprecatedParamWithDefaultEvaluate );
        PropertiesUtil.serialize( props, "deprecatedArray", deprecatedArray );
        PropertiesUtil.serialize( props, "deprecatedArrayWithDefaults", deprecatedArrayWithDefaults );
        PropertiesUtil.serialize( props, "deprecatedProperties", deprecatedProperties );
        PropertiesUtil.serialize( props, "deprecatedList", deprecatedList );
        PropertiesUtil.serialize( props, "deprecatedListWithDefaults", deprecatedListWithDefaults );
        PropertiesUtil.serialize( props, "deprecatedMap", deprecatedMap );
    }

}
