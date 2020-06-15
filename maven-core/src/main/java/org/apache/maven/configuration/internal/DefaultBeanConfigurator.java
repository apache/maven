package org.apache.maven.configuration.internal;

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
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.configuration.BeanConfigurationException;
import org.apache.maven.configuration.BeanConfigurationPathTranslator;
import org.apache.maven.configuration.BeanConfigurationRequest;
import org.apache.maven.configuration.BeanConfigurationValuePreprocessor;
import org.apache.maven.configuration.BeanConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * <strong>Warning:</strong> This is an internal class that is only public for technical reasons, it is not part of the
 * public API. In particular, this class can be changed or deleted without prior notice.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultBeanConfigurator
    implements BeanConfigurator
{

    private final ConverterLookup converterLookup = new DefaultConverterLookup();

    public void configureBean( BeanConfigurationRequest request )
        throws BeanConfigurationException
    {
        Objects.requireNonNull( request, "request cannot be null" );
        Objects.requireNonNull( request.getBean(), "request.bean cannot be null" );

        Object configuration = request.getConfiguration();
        if ( configuration == null )
        {
            return;
        }

        PlexusConfiguration plexusConfig;
        if ( configuration instanceof PlexusConfiguration )
        {
            plexusConfig = (PlexusConfiguration) configuration;
        }
        else if ( configuration instanceof Xpp3Dom )
        {
            plexusConfig = new XmlPlexusConfiguration( (Xpp3Dom) configuration );
        }
        else
        {
            throw new BeanConfigurationException( "unsupported bean configuration source ("
                + configuration.getClass().getName() + ")" );
        }

        if ( request.getConfigurationElement() != null )
        {
            plexusConfig = plexusConfig.getChild( request.getConfigurationElement() );
        }

        ClassLoader classLoader = request.getClassLoader();
        if ( classLoader == null )
        {
            classLoader = request.getBean().getClass().getClassLoader();
        }

        BeanExpressionEvaluator evaluator = new BeanExpressionEvaluator( request );

        ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();

        try
        {
            converter.processConfiguration( converterLookup, request.getBean(), classLoader, plexusConfig, evaluator );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new BeanConfigurationException( e.getMessage(), e );
        }
    }

    static class BeanExpressionEvaluator
        implements TypeAwareExpressionEvaluator
    {

        private final BeanConfigurationValuePreprocessor preprocessor;

        private final BeanConfigurationPathTranslator translator;

        BeanExpressionEvaluator( BeanConfigurationRequest request )
        {
            preprocessor = request.getValuePreprocessor();
            translator = request.getPathTranslator();
        }

        public Object evaluate( String expression, Class<?> type )
            throws ExpressionEvaluationException
        {
            if ( preprocessor != null )
            {
                try
                {
                    return preprocessor.preprocessValue( expression, type );
                }
                catch ( BeanConfigurationException e )
                {
                    throw new ExpressionEvaluationException( e.getMessage(), e );
                }
            }
            return expression;
        }

        public Object evaluate( String expression )
            throws ExpressionEvaluationException
        {
            return evaluate( expression, null );
        }

        public File alignToBaseDirectory( File file )
        {
            if ( translator != null )
            {
                return translator.translatePath( file );
            }
            return file;
        }

    }

}
