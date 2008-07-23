package org.apache.maven.project.interpolation;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Use a regular expression search to find and resolve expressions within the POM.
 *
 * @author jdcasey Created on Feb 3, 2005
 * @version $Id$
 * @todo Consolidate this logic with the PluginParameterExpressionEvaluator, minus deprecations/bans.
 */
public class RegexBasedModelInterpolator
    extends AbstractLogEnabled
    implements ModelInterpolator
{

    private static final List<String> PROJECT_PREFIXES = Arrays.asList( new String[]{ "pom.", "project." } );
    private static final List<String> TRANSLATED_PATH_EXPRESSIONS;

    static
    {
        List<String> translatedPrefixes = new ArrayList<String>();

        // MNG-1927, MNG-2124, MNG-3355:
        // If the build section is present and the project directory is non-null, we should make
        // sure interpolation of the directories below uses translated paths.
        // Afterward, we'll double back and translate any paths that weren't covered during interpolation via the
        // code below...
        translatedPrefixes.add( "build.directory" );
        translatedPrefixes.add( "build.outputDirectory" );
        translatedPrefixes.add( "build.testOutputDirectory" );
        translatedPrefixes.add( "build.sourceDirectory" );
        translatedPrefixes.add( "build.testSourceDirectory" );

        TRANSLATED_PATH_EXPRESSIONS = translatedPrefixes;
    }

    private PathTranslator pathTranslator;

    public RegexBasedModelInterpolator()
        throws IOException
    {
    }
    
    // for testing.
    protected RegexBasedModelInterpolator( PathTranslator pathTranslator )
    {
        this.pathTranslator = pathTranslator;
    }

    public Model interpolate( Model model, Map<String, ?> context )
        throws ModelInterpolationException
    {
        Properties props = new Properties();
        props.putAll( context );

        return interpolate( model,
                            null,
                            new DefaultProjectBuilderConfiguration().setExecutionProperties( props ),
                            true );
    }

    public Model interpolate( Model model, Map<String, ?> context, boolean strict )
        throws ModelInterpolationException
    {
        Properties props = new Properties();
        props.putAll( context );

        return interpolate( model,
                            null,
                            new DefaultProjectBuilderConfiguration().setExecutionProperties( props ),
                            true );
    }

    /**
     * Serialize the inbound Model instance to a StringWriter, perform the regex replacement to resolve
     * POM expressions, then re-parse into the resolved Model instance.
     * <br/>
     * <b>NOTE:</b> This will result in a different instance of Model being returned!!!
     *
     * @param model   The inbound Model instance, to serialize and reference for expression resolution
     * @param context The other context map to be used during resolution
     * @param overrideContext The context map which should be used to OVERRIDE
     *                        values from everything else. This will come from the CLI
     *                        or userProperties in the execution request.
     * @param projectDir The directory from which the current model's pom was read.
     * @param strict  This parameter is ignored!
     * @param debugMessages If true, print any feedback from the interpolator out to the DEBUG log-level.
     * @return The resolved instance of the inbound Model. This is a different instance!
     */
    public Model interpolate( Model model, File projectDir, ProjectBuilderConfiguration config, boolean debugMessages )
        throws ModelInterpolationException
    {
        StringWriter sWriter = new StringWriter();

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            writer.write( sWriter, model );
        }
        catch ( IOException e )
        {
            throw new ModelInterpolationException( "Cannot serialize project model for interpolation.", e );
        }

        String serializedModel = sWriter.toString();
        serializedModel = interpolate( serializedModel, model, projectDir, config, debugMessages );

        StringReader sReader = new StringReader( serializedModel );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        try
        {
            model = modelReader.read( sReader );
        }
        catch ( IOException e )
        {
            throw new ModelInterpolationException(
                "Cannot read project model from interpolating filter of serialized version.", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ModelInterpolationException(
                "Cannot read project model from interpolating filter of serialized version.", e );
        }

        return model;
    }

    /**
     * Interpolates all expressions in the src parameter.
     * <p>
     * The algorithm used for each expression is:
     * <ul>
     *   <li>If it starts with either "pom." or "project.", the expression is evaluated against the model.</li>
     *   <li>If the value is null, get the value from the context.</li>
     *   <li>If the value is null, but the context contains the expression, don't replace the expression string
     *       with the value, and continue to find other expressions.</li>
     *   <li>If the value is null, get it from the model properties.</li>
     *   <li>
     * @param overrideContext
     * @param debugMessages
     */
    public String interpolate( String src,
                                        Model model,
                                        final File projectDir,
                                        ProjectBuilderConfiguration config,
                                        boolean debugMessages )
        throws ModelInterpolationException
    {
        Logger logger = getLogger();

        String timestampFormat = DEFAULT_BUILD_TIMESTAMP_FORMAT;

        Properties modelProperties = model.getProperties();
        if ( modelProperties != null )
        {
            timestampFormat = modelProperties.getProperty( BUILD_TIMESTAMP_FORMAT_PROPERTY, timestampFormat );
        }

        ValueSource modelValueSource1 = new PrefixedObjectValueSource( PROJECT_PREFIXES, model, false );
        ValueSource modelValueSource2 = new ObjectBasedValueSource( model );

        ValueSource basedirValueSource = new PrefixedValueSourceWrapper( new ValueSource(){
            public Object getValue( String expression )
            {
                if ( projectDir != null && "basedir".equals( expression ) )
                {
                    return projectDir.getAbsolutePath();
                }

                return null;
            }
        },
        PROJECT_PREFIXES, true );

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        // NOTE: Order counts here!
        interpolator.addValueSource( basedirValueSource );
        interpolator.addValueSource( new BuildTimestampValueSource( config.getBuildStartTime(), timestampFormat ) );
        interpolator.addValueSource( new MapBasedValueSource( config.getExecutionProperties() ) );
        interpolator.addValueSource( modelValueSource1 );
        interpolator.addValueSource( new PrefixedValueSourceWrapper( new MapBasedValueSource( modelProperties ), PROJECT_PREFIXES, true ) );
        interpolator.addValueSource( modelValueSource2 );
        interpolator.addValueSource( new MapBasedValueSource( config.getUserProperties() ) );
        
        PathTranslatingPostProcessor pathTranslatingPostProcessor =
            new PathTranslatingPostProcessor( TRANSLATED_PATH_EXPRESSIONS, projectDir, pathTranslator );
        
        interpolator.addPostProcessor( pathTranslatingPostProcessor );

        RecursionInterceptor recursionInterceptor = new PrefixAwareRecursionInterceptor( PROJECT_PREFIXES );

        String result = src;
        try
        {
            result = interpolator.interpolate( result, "", recursionInterceptor );
        }
        catch( InterpolationException e )
        {
            throw new ModelInterpolationException( e.getMessage(), e );
        }

        if ( debugMessages )
        {
            List<?> feedback = interpolator.getFeedback();
            if ( feedback != null && !feedback.isEmpty() )
            {
                logger.debug( "Maven encountered the following problems during initial POM interpolation:" );

                Object last = null;
                for ( Iterator<?> it = feedback.iterator(); it.hasNext(); )
                {
                    Object next = it.next();

                    if ( next instanceof Throwable )
                    {
                        if ( last == null )
                        {
                            logger.debug( "", ( (Throwable) next ) );
                        }
                        else
                        {
                            logger.debug( String.valueOf( last ), ( (Throwable) next ) );
                        }
                    }
                    else
                    {
                        if ( last != null )
                        {
                            logger.debug( String.valueOf( last ) );
                        }

                        last = next;
                    }
                }

                if ( last != null )
                {
                    logger.debug( String.valueOf( last ) );
                }
            }
        }

        interpolator.clearFeedback();

        return result;
    }

}
