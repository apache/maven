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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Dumps this mojo's configuration into a properties file.
 *
 * @author Benjamin Bentmann
 *
 * @goal config
 * @phase validate
 */
public class ConfigMojo
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
     * A parameter with an alias.
     *
     * @parameter alias="aliasParamLegacy"
     */
    private String aliasParam;

    /**
     * A parameter with a constant default value.
     *
     * @parameter default-value="maven-core-it"
     */
    private String defaultParam;

    /**
     * A parameter with a default value using multiple expressions.
     *
     * @parameter default-value="${project.groupId}:${project.artifactId}:${project.version}"
     */
    private String defaultParamWithExpression;

    /**
     * A parameter that combines all of the annotations.
     *
     * @parameter alias="fullyAnnotatedParam" property="config.aliasDefaultExpressionParam" default-value="test"
     */
    private String aliasDefaultExpressionParam;

    /**
     * A simple parameter of type {@link java.lang.Boolean}.
     *
     * @parameter property="config.booleanParam"
     */
    private Boolean booleanParam;

    /**
     * A simple parameter of type {@link java.lang.Boolean#TYPE}.
     *
     * @parameter property="config.primitiveBooleanParam"
     */
    private boolean primitiveBooleanParam;

    /**
     * A simple parameter of type {@link java.lang.Byte}.
     *
     * @parameter property="config.byteParam"
     */
    private Byte byteParam;

    /**
     * A simple parameter of type {@link java.lang.Short}.
     *
     * @parameter property="config.shortParam"
     */
    private Short shortParam;

    /**
     * A simple parameter of type {@link java.lang.Integer}.
     *
     * @parameter property="config.integerParam"
     */
    private Integer integerParam;

    /**
     * A simple parameter of type {@link java.lang.Integer#TYPE}.
     *
     * @parameter property="config.primitiveIntegerParam"
     */
    private int primitiveIntegerParam;

    /**
     * A simple parameter of type {@link java.lang.Long}.
     *
     * @parameter property="config.longParam"
     */
    private Long longParam;

    /**
     * A simple parameter of type {@link java.lang.Float}.
     *
     * @parameter property="config.floatParam"
     */
    private Float floatParam;

    /**
     * A simple parameter of type {@link java.lang.Double}.
     *
     * @parameter property="config.doubleParam"
     */
    private Double doubleParam;

    /**
     * A simple parameter of type {@link java.lang.Character}.
     *
     * @parameter property="config.characterParam"
     */
    private Character characterParam;

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
     * A simple parameter of type {@link java.util.Date}.
     *
     * @parameter property="config.dateParam"
     */
    private Date dateParam;

    /**
     * A simple parameter of type {@link java.net.URL}.
     *
     * @parameter property="config.urlParam"
     */
    private URL urlParam;

    /**
     * A simple parameter of type {@link java.net.URI} (requires Maven 3.x).
     *
     * @parameter
     */
    private URI uriParam;

    /**
     * An array parameter of component type {@link java.lang.String}.
     *
     * @parameter
     */
    private String[] stringParams;

    /**
     * An array parameter of component type {@link java.io.File}.
     *
     * @parameter
     */
    private File[] fileParams;

    /**
     * A collection parameter of type {@link java.util.List}.
     *
     * @parameter
     */
    private List listParam;

    /**
     * A collection parameter of type {@link java.util.Set}.
     *
     * @parameter
     */
    private Set setParam;

    /**
     * A collection parameter of type {@link java.util.Map}.
     *
     * @parameter
     */
    private Map mapParam;

    /**
     * A collection parameter of type {@link java.util.Properties}.
     *
     * @parameter
     */
    private Properties propertiesParam;

    /**
     * A complex parameter with an alias.
     *
     * @parameter alias="aliasStringParamsLegacy"
     */
    private String[] aliasStringParams;

    /**
     * A complex parameter of type {@link org.apache.maven.plugin.coreit.Bean}.
     *
     * @parameter
     */
    private Bean beanParam;

    /**
     * A raw DOM snippet.
     *
     * @parameter
     */
    private PlexusConfiguration domParam;

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
        PropertiesUtil.serialize( props, "aliasParam", aliasParam );
        PropertiesUtil.serialize( props, "defaultParam", defaultParam );
        PropertiesUtil.serialize( props, "defaultParamWithExpression", defaultParamWithExpression );
        PropertiesUtil.serialize( props, "aliasDefaultExpressionParam", aliasDefaultExpressionParam );
        PropertiesUtil.serialize( props, "booleanParam", booleanParam );
        if ( primitiveBooleanParam )
        {
            PropertiesUtil.serialize( props, "primitiveBooleanParam", primitiveBooleanParam );
        }
        PropertiesUtil.serialize( props, "byteParam", byteParam );
        PropertiesUtil.serialize( props, "shortParam", shortParam );
        PropertiesUtil.serialize( props, "integerParam", integerParam );
        if ( primitiveIntegerParam != 0 )
        {
            PropertiesUtil.serialize( props, "primitiveIntegerParam", primitiveIntegerParam );
        }
        PropertiesUtil.serialize( props, "longParam", longParam );
        PropertiesUtil.serialize( props, "floatParam", floatParam );
        PropertiesUtil.serialize( props, "doubleParam", doubleParam );
        PropertiesUtil.serialize( props, "characterParam", characterParam );
        PropertiesUtil.serialize( props, "stringParam", stringParam );
        PropertiesUtil.serialize( props, "fileParam", fileParam );
        PropertiesUtil.serialize( props, "dateParam", dateParam );
        PropertiesUtil.serialize( props, "urlParam", urlParam );
        PropertiesUtil.serialize( props, "uriParam", uriParam );
        PropertiesUtil.serialize( props, "stringParams", stringParams );
        PropertiesUtil.serialize( props, "fileParams", fileParams );
        PropertiesUtil.serialize( props, "listParam", listParam );
        PropertiesUtil.serialize( props, "setParam", setParam );
        PropertiesUtil.serialize( props, "mapParam", mapParam );
        PropertiesUtil.serialize( props, "propertiesParam", propertiesParam );
        PropertiesUtil.serialize( props, "aliasStringParams", aliasStringParams );
        PropertiesUtil.serialize( props, "domParam", domParam );
        if ( beanParam != null )
        {
            PropertiesUtil.serialize( props, "beanParam.fieldParam", beanParam.fieldParam );
            PropertiesUtil.serialize( props, "beanParam.setterParam", beanParam.setterParam );
            PropertiesUtil.serialize( props, "beanParam.setterCalled", beanParam.setterCalled );
        }
    }

}
