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
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
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
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "\\$\\{(pom\\.|project\\.|env\\.)?([^}]+)\\}" );

    private Properties envars;

    public RegexBasedModelInterpolator( Properties envars )
    {
        this.envars = envars;
    }

    public RegexBasedModelInterpolator()
        throws IOException
    {
        envars = CommandLineUtils.getSystemEnvVars();
    }

    public Model interpolate( Model model, Map context )
        throws ModelInterpolationException
    {
        return interpolate( model, context, true );
    }

    /**
     * Serialize the inbound Model instance to a StringWriter, perform the regex replacement to resolve
     * POM expressions, then re-parse into the resolved Model instance.
     * <br/>
     * <b>NOTE:</b> This will result in a different instance of Model being returned!!!
     *
     * @param model   The inbound Model instance, to serialize and reference for expression resolution
     * @param context The other context map to be used during resolution
     * @return The resolved instance of the inbound Model. This is a different instance!
     */
    public Model interpolate( Model model, Map context, boolean strict )
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
        serializedModel = interpolateInternal( serializedModel, model, context );

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

    private String interpolateInternal( String src, Model model, Map context )
        throws ModelInterpolationException
    {
        String result = src;
        Matcher matcher = EXPRESSION_PATTERN.matcher( result );
        while ( matcher.find() )
        {
            String wholeExpr = matcher.group( 0 );
            String realExpr = matcher.group( 2 );

            Object value = context.get( realExpr );

            if ( value == null )
            {
                // This may look out of place, but its here for the MNG-2124/MNG-1927 fix described in the project builder
                if ( context.containsKey( realExpr ) )
                {
                    // It existed, but was null. Leave it alone.
                    continue;
                }

                value = model.getProperties().getProperty( realExpr );
            }

            if ( value == null )
            {
                try
                {
                    // NOTE: We've already trimmed off any leading expression parts like 'project.'
                    // or 'pom.', and now we have to ensure that the ReflectionValueExtractor
                    // doesn't try to do it again.
                    value = ReflectionValueExtractor.evaluate( realExpr, model, false );
                }
                catch ( Exception e )
                {
                    Logger logger = getLogger();
                    if ( logger != null )
                    {
                        logger.debug( "POM interpolation cannot proceed with expression: " + wholeExpr + ". Skipping...", e );
                    }
                }
            }

            if ( value == null )
            {
                value = envars.getProperty( realExpr );
            }

            // if the expression refers to itself, skip it.
            if ( String.valueOf( value ).indexOf( wholeExpr ) > -1 )
            {
                throw new ModelInterpolationException( wholeExpr, "Expression value '" + value + "' references itself in '" + model.getId() + "'." );
            }

            if ( value != null )
            {
                result = StringUtils.replace( result, wholeExpr, String.valueOf( value ) );
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

        return result;
    }

}
