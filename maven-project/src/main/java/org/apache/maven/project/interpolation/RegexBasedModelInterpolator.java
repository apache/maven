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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "\\$\\{([^}]+)\\}" );

    private static final List PROJECT_PREFIXES = Arrays.asList( new String[]{ "pom.", "project." } );
    private static final List TRANSLATED_PATH_EXPRESSIONS;

    static
    {
        List translatedPrefixes = new ArrayList();

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

    public Model interpolate( Model model, Map context )
        throws ModelInterpolationException
    {
        return interpolate( model, context, Collections.EMPTY_MAP, null, true );
    }

    public Model interpolate( Model model, Map context, boolean strict )
        throws ModelInterpolationException
    {
        return interpolate( model, context, Collections.EMPTY_MAP, null, true );
    }

    public Model interpolate( Model model,
                              Map context,
                              Map overrideContext,
                              boolean strict )
        throws ModelInterpolationException
    {
        return interpolate( model, context, Collections.EMPTY_MAP, null, true );
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
     * @param outputDebugMessages If true, print any feedback from the interpolator out to the DEBUG log-level.
     * @return The resolved instance of the inbound Model. This is a different instance!
     */
    public Model interpolate( Model model, Map context, Map overrideContext, File projectDir, boolean outputDebugMessages )
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
        serializedModel = interpolateInternal( serializedModel, model, context, overrideContext, projectDir, outputDebugMessages );

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
     * @param outputDebugMessages
     */
    private String interpolateInternal( String src,
                                        Model model,
                                        Map context,
                                        Map overrideContext,
                                        final File projectDir,
                                        boolean outputDebugMessages )
        throws ModelInterpolationException
    {
        Logger logger = getLogger();

        ValueSource baseModelValueSource1 = new PrefixedObjectValueSource( PROJECT_PREFIXES, model, false );
        ValueSource modelValueSource1 = new PathTranslatingValueSource( baseModelValueSource1,
                                                                       TRANSLATED_PATH_EXPRESSIONS,
                                                                       projectDir, pathTranslator );

        ValueSource baseModelValueSource2 = new ObjectBasedValueSource( model );
        ValueSource modelValueSource2 = new PathTranslatingValueSource( baseModelValueSource2,
                                                                       TRANSLATED_PATH_EXPRESSIONS,
                                                                       projectDir, pathTranslator );

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
        interpolator.addValueSource( new MapBasedValueSource( overrideContext ) );
        interpolator.addValueSource( modelValueSource1 );
        interpolator.addValueSource( new PrefixedValueSourceWrapper( new MapBasedValueSource( model.getProperties() ), PROJECT_PREFIXES, true ) );
        interpolator.addValueSource( modelValueSource2 );
        interpolator.addValueSource( new MapBasedValueSource( context ) );

        RecursionInterceptor recursionInterceptor = new PrefixAwareRecursionInterceptor( PROJECT_PREFIXES );

        String result = src;
        Matcher matcher = EXPRESSION_PATTERN.matcher( result );
        while ( matcher.find() )
        {
            String wholeExpr = matcher.group( 0 );

            Object value;
            try
            {
                value = interpolator.interpolate( wholeExpr, "", recursionInterceptor );
            }
            catch( InterpolationException e )
            {
                throw new ModelInterpolationException( e.getMessage(), e );
            }

            if ( value == null || value.equals(  wholeExpr ) )
            {
                continue;
            }

            // FIXME: Does this account for the case where ${project.build.directory} -> ${build.directory}??
            if ( value != null )
            {
                // if the expression refers to itself, skip it.
                // replace project. expressions with pom. expressions to circumvent self-referencing expressions using
                // the 2 different model expressions.
                if ( StringUtils.replace( value.toString(), "${project.", "${pom." ).indexOf(
                    StringUtils.replace( wholeExpr, "${project.", "${pom." ) ) > -1 )
                {
                    throw new ModelInterpolationException( wholeExpr, "Expression value '" + value
                        + "' references itself in '" + model.getId() + "'." );
                }

                result = StringUtils.replace( result, wholeExpr, value.toString() );
                // could use:
                // result = matcher.replaceFirst( stringValue );
                // but this could result in multiple lookups of stringValue, and replaceAll is not correct behaviour
                matcher.reset( result );
            }

/*
        // This is the desired behaviour, however there are too many crappy poms in the repo and an issue with the
        // timing of executing the interpolation

            else
            {
                throw new ModelInterpolationException(
                    "Expression '" + wholeExpr + "' did not evaluate to anything in the model" );
            }
*/
        }

        if ( outputDebugMessages )
        {
            List feedback = interpolator.getFeedback();
            if ( feedback != null && !feedback.isEmpty() )
            {
                logger.debug( "Maven encountered the following problems during initial POM interpolation:" );

                Object last = null;
                for ( Iterator it = feedback.iterator(); it.hasNext(); )
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
