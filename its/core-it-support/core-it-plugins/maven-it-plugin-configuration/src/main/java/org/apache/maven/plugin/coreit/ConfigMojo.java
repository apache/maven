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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Dumps this mojo's configuration into a properties file.
 * 
 * @goal config
 * @phase validate
 * 
 * @author Benjamin Bentmann
 * @version $Id$
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
     * @parameter expression="${config.propertiesFile}"
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
     * @parameter alias="fullyAnnotatedParam" expression="${config.aliasDefaultExpressionParam}" default-value="test"
     */
    private String aliasDefaultExpressionParam;

    /**
     * A simple parameter of type {@link java.lang.Boolean}.
     * 
     * @parameter expression="${config.booleanParam}"
     */
    private Boolean booleanParam;

    /**
     * A simple parameter of type {@link java.lang.Boolean#TYPE}.
     * 
     * @parameter expression="${config.primitiveBooleanParam}"
     */
    private boolean primitiveBooleanParam;

    /**
     * A simple parameter of type {@link java.lang.Byte}.
     * 
     * @parameter expression="${config.byteParam}"
     */
    private Byte byteParam;

    /**
     * A simple parameter of type {@link java.lang.Short}.
     * 
     * @parameter expression="${config.shortParam}"
     */
    private Short shortParam;

    /**
     * A simple parameter of type {@link java.lang.Integer}.
     * 
     * @parameter expression="${config.intergerParam}"
     */
    private Integer integerParam;

    /**
     * A simple parameter of type {@link java.lang.Integer#TYPE}.
     * 
     * @parameter expression="${config.primitiveIntegerParam}"
     */
    private int primitiveIntegerParam;

    /**
     * A simple parameter of type {@link java.lang.Long}.
     * 
     * @parameter expression="${config.longParam}"
     */
    private Long longParam;

    /**
     * A simple parameter of type {@link java.lang.Float}.
     * 
     * @parameter expression="${config.floatParam}"
     */
    private Float floatParam;

    /**
     * A simple parameter of type {@link java.lang.Double}.
     * 
     * @parameter expression="${config.doubleParam}"
     */
    private Double doubleParam;

    /**
     * A simple parameter of type {@link java.lang.Character}.
     * 
     * @parameter expression="${config.characterParam}"
     */
    private Character characterParam;

    /**
     * A simple parameter of type {@link java.lang.String}.
     * 
     * @parameter expression="${config.stringParam}"
     */
    private String stringParam;

    /**
     * A simple parameter of type {@link java.io.File}.
     * 
     * @parameter expression="${config.fileParam}"
     */
    private File fileParam;

    /**
     * A simple parameter of type {@link java.util.Date}.
     * 
     * @parameter expression="${config.dateParam}"
     */
    private Date dateParam;

    /**
     * A simple parameter of type {@link java.net.URL}.
     * 
     * @parameter expression="${config.urlParam}"
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

        OutputStream out = null;
        try
        {
            propertiesFile.getParentFile().mkdirs();
            out = new FileOutputStream( propertiesFile );
            configProps.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + propertiesFile, e );
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }

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
        dumpValue( props, "propertiesFile", propertiesFile );
        dumpValue( props, "aliasParam", aliasParam );
        dumpValue( props, "defaultParam", defaultParam );
        dumpValue( props, "defaultParamWithExpression", defaultParamWithExpression );
        dumpValue( props, "aliasDefaultExpressionParam", aliasDefaultExpressionParam );
        dumpValue( props, "booleanParam", booleanParam );
        if ( primitiveBooleanParam )
        {
            dumpValue( props, "primitiveBooleanParam", Boolean.valueOf( primitiveBooleanParam ) );
        }
        dumpValue( props, "byteParam", byteParam );
        dumpValue( props, "shortParam", shortParam );
        dumpValue( props, "integerParam", integerParam );
        if ( primitiveIntegerParam != 0 )
        {
            dumpValue( props, "primitiveIntegerParam", new Integer( primitiveIntegerParam ) );
        }
        dumpValue( props, "longParam", longParam );
        dumpValue( props, "floatParam", floatParam );
        dumpValue( props, "doubleParam", doubleParam );
        dumpValue( props, "characterParam", characterParam );
        dumpValue( props, "stringParam", stringParam );
        dumpValue( props, "fileParam", fileParam );
        dumpValue( props, "dateParam", dateParam );
        dumpValue( props, "urlParam", urlParam );
        dumpValue( props, "uriParam", uriParam );
        dumpValue( props, "stringParams", stringParams );
        dumpValue( props, "fileParams", fileParams );
        dumpValue( props, "listParam", listParam );
        dumpValue( props, "setParam", setParam );
        dumpValue( props, "mapParam", mapParam );
        dumpValue( props, "propertiesParam", propertiesParam );
        dumpValue( props, "domParam", domParam );
        if ( beanParam != null )
        {
            dumpValue( props, "beanParam.fieldParam", beanParam.fieldParam );
            dumpValue( props, "beanParam.setterParam", beanParam.setterParam );
            dumpValue( props, "beanParam.setterCalled", Boolean.valueOf( beanParam.setterCalled ) );
        }
    }

    private void dumpValue( Properties props, String key, Object value )
    {
        if ( value != null && value.getClass().isArray() )
        {
            props.setProperty( key, Integer.toString( Array.getLength( value ) ) );
            for ( int i = Array.getLength( value ) - 1; i >= 0; i-- )
            {
                dumpValue( props, key + "." + i, Array.get( value, i ) );
            }
        }
        else if ( value instanceof Collection )
        {
            Collection collection = (Collection) value;
            props.setProperty( key, Integer.toString( collection.size() ) );
            int i = 0;
            for ( Iterator it = collection.iterator(); it.hasNext(); i++ )
            {
                dumpValue( props, key + "." + i, it.next() );
            }
        }
        else if ( value instanceof Map )
        {
            Map map = (Map) value;
            props.setProperty( key, Integer.toString( map.size() ) );
            int i = 0;
            for ( Iterator it = map.keySet().iterator(); it.hasNext(); i++ )
            {
                Object k = it.next();
                Object v = map.get( k );
                dumpValue( props, key + "." + k, v );
            }
        }
        else if ( value instanceof PlexusConfiguration )
        {
            PlexusConfiguration config = (PlexusConfiguration) value;

            String val = config.getValue( null );
            if ( val != null )
            {
                props.setProperty( key + ".value", val );
            }

            String[] attributes = config.getAttributeNames();
            props.setProperty( key + ".attributes", Integer.toString( attributes.length ) );
            for ( int i = attributes.length - 1; i >= 0; i-- )
            {
                props.setProperty( key + ".attributes." + attributes[i], config.getAttribute( attributes[i], "" ) );
            }

            PlexusConfiguration children[] = config.getChildren();
            props.setProperty( key + ".children", Integer.toString( children.length ) );
            Map indices = new HashMap();
            for ( int i = 0; i < children.length; i++ )
            {
                PlexusConfiguration child = children[i];
                String name = child.getName();
                Integer index = (Integer) indices.get( name );
                if ( index == null )
                {
                    index = new Integer( 0 );
                }
                dumpValue( props, key + ".children." + name + "." + index, child );
                indices.put( name, new Integer( index.intValue() + 1 ) );
            }
        }
        else if ( value instanceof Date )
        {
            props.setProperty( key, new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format( (Date) value ) );
        }
        else if ( value != null )
        {
            props.setProperty( key, value.toString() );
        }
    }

}
