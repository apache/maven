package org.apache.maven.model.interpolation;

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
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.UrlNormalizer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * Use a regular expression search to find and resolve expressions within the POM.
 *
 * @author jdcasey Created on Feb 3, 2005
 */
public abstract class AbstractStringBasedModelInterpolator
    implements ModelInterpolator
{

    /**
     * The default format used for build timestamps.
     */
    static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyyMMdd-HHmm";

    /**
     * The name of a property that if present in the model's {@code <properties>} section specifies a custom format for
     * build timestamps. See {@link java.text.SimpleDateFormat} for details on the format.
     */
    private static final String BUILD_TIMESTAMP_FORMAT_PROPERTY = "maven.build.timestamp.format";

    private static final List<String> PROJECT_PREFIXES = Arrays.asList( new String[]{ "pom.", "project." } );

    private static final Collection<String> TRANSLATED_PATH_EXPRESSIONS;

    static
    {
        Collection<String> translatedPrefixes = new HashSet<String>();

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
        translatedPrefixes.add( "build.scriptSourceDirectory" );
        translatedPrefixes.add( "reporting.outputDirectory" );

        TRANSLATED_PATH_EXPRESSIONS = translatedPrefixes;
    }

    @Requirement
    private PathTranslator pathTranslator;

    @Requirement
    private UrlNormalizer urlNormalizer;

    private Interpolator interpolator;

    private RecursionInterceptor recursionInterceptor;

    public AbstractStringBasedModelInterpolator()
    {
        interpolator = createInterpolator();
        recursionInterceptor = new PrefixAwareRecursionInterceptor( PROJECT_PREFIXES );
    }

    public AbstractStringBasedModelInterpolator setPathTranslator( PathTranslator pathTranslator )
    {
        this.pathTranslator = pathTranslator;
        return this;
    }

    public AbstractStringBasedModelInterpolator setUrlNormalizer( UrlNormalizer urlNormalizer )
    {
        this.urlNormalizer = urlNormalizer;
        return this;
    }

    protected List<ValueSource> createValueSources( final Model model, final File projectDir,
                                                    final ModelBuildingRequest config,
                                                    final ModelProblemCollector problems )
    {
        Properties modelProperties = model.getProperties();

        ValueSource modelValueSource1 = new PrefixedObjectValueSource( PROJECT_PREFIXES, model, false );
        if ( config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            modelValueSource1 = new ProblemDetectingValueSource( modelValueSource1, "pom.", "project.", problems );
        }

        ValueSource modelValueSource2 = new ObjectBasedValueSource( model );
        if ( config.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 )
        {
            modelValueSource2 = new ProblemDetectingValueSource( modelValueSource2, "", "project.", problems );
        }

        // NOTE: Order counts here!
        List<ValueSource> valueSources = new ArrayList<ValueSource>( 9 );

        if ( projectDir != null )
        {
            ValueSource basedirValueSource = new PrefixedValueSourceWrapper( new AbstractValueSource( false )
            {
                public Object getValue( String expression )
                {
                    if ( "basedir".equals( expression ) )
                    {
                        return projectDir.getAbsolutePath();
                    }
                    return null;
                }
            }, PROJECT_PREFIXES, true );
            valueSources.add( basedirValueSource );

            ValueSource baseUriValueSource = new PrefixedValueSourceWrapper( new AbstractValueSource( false )
            {
                public Object getValue( String expression )
                {
                    if ( "baseUri".equals( expression ) )
                    {
                        return projectDir.getAbsoluteFile().toURI().toString();
                    }
                    return null;
                }
            }, PROJECT_PREFIXES, false );
            valueSources.add( baseUriValueSource );

            String timestampFormat = DEFAULT_BUILD_TIMESTAMP_FORMAT;
            if ( modelProperties != null )
            {
                timestampFormat = modelProperties.getProperty( BUILD_TIMESTAMP_FORMAT_PROPERTY, timestampFormat );
            }
            valueSources.add( new BuildTimestampValueSource( config.getBuildStartTime(), timestampFormat ) );
        }

        valueSources.add( modelValueSource1 );

        valueSources.add( new MapBasedValueSource( config.getUserProperties() ) );

        valueSources.add( new MapBasedValueSource( modelProperties ) );

        valueSources.add( new MapBasedValueSource( config.getSystemProperties() ) );

        valueSources.add( new AbstractValueSource( false )
        {
            public Object getValue( String expression )
            {
                return config.getSystemProperties().getProperty( "env." + expression );
            }
        } );

        valueSources.add( modelValueSource2 );

        return valueSources;
    }

    protected List<? extends InterpolationPostProcessor> createPostProcessors( final Model model,
                                                                               final File projectDir,
                                                                               final ModelBuildingRequest config )
    {
        List<InterpolationPostProcessor> processors = new ArrayList<InterpolationPostProcessor>( 2 );
        if ( projectDir != null )
        {
            processors.add( new PathTranslatingPostProcessor( PROJECT_PREFIXES, TRANSLATED_PATH_EXPRESSIONS,
                                                              projectDir, pathTranslator ) );
        }
        processors.add( new UrlNormalizingPostProcessor( urlNormalizer ) );
        return processors;
    }

    protected String interpolateInternal( String src, List<? extends ValueSource> valueSources,
                                          List<? extends InterpolationPostProcessor> postProcessors,
                                          ModelProblemCollector problems )
    {
        if ( src.indexOf( "${" ) < 0 )
        {
            return src;
        }

        String result = src;
        synchronized ( this )
        {

            for ( ValueSource vs : valueSources )
            {
                interpolator.addValueSource( vs );
            }

            for ( InterpolationPostProcessor postProcessor : postProcessors )
            {
                interpolator.addPostProcessor( postProcessor );
            }

            try
            {
                try
                {
                    result = interpolator.interpolate( result, recursionInterceptor );
                }
                catch ( InterpolationException e )
                {
                    problems.add( Severity.ERROR, e.getMessage(), null, e );
                }

                interpolator.clearFeedback();
            }
            finally
            {
                for ( ValueSource vs : valueSources )
                {
                    interpolator.removeValuesSource( vs );
                }

                for ( InterpolationPostProcessor postProcessor : postProcessors )
                {
                    interpolator.removePostProcessor( postProcessor );
                }
            }
        }

        return result;
    }

    protected RecursionInterceptor getRecursionInterceptor()
    {
        return recursionInterceptor;
    }

    protected void setRecursionInterceptor( RecursionInterceptor recursionInterceptor )
    {
        this.recursionInterceptor = recursionInterceptor;
    }

    protected abstract Interpolator createInterpolator();

    protected final Interpolator getInterpolator()
    {
        return interpolator;
    }

}
