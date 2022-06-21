package org.apache.maven.plugin.internal;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Print warnings if read-only parameters of a plugin are used in configuration.
 *
 * @author Slawomir Jaranowski
 */
@Named
@Singleton
public class ReadOnlyPluginParametersValidator extends AbstractMavenPluginParametersValidator
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ReadOnlyPluginParametersValidator.class );

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    @Override
    protected String getParameterLogReason( Parameter parameter )
    {
        return "is read-only, must not be used in configuration";
    }

    @Override
    public void validate( MojoDescriptor mojoDescriptor, PlexusConfiguration pomConfiguration,
                          ExpressionEvaluator expressionEvaluator )
    {
        if ( !LOGGER.isWarnEnabled() )
        {
            return;
        }

        mojoDescriptor.getParameters().stream()
            .filter( parameter -> !parameter.isEditable() )
            .forEach( parameter -> checkParameter( parameter, pomConfiguration, expressionEvaluator ) );
    }

    protected void checkParameter( Parameter parameter,
                                   PlexusConfiguration pomConfiguration,
                                   ExpressionEvaluator expressionEvaluator )
    {
        PlexusConfiguration config = pomConfiguration.getChild( parameter.getName(), false );

        if ( isValueSet( config, expressionEvaluator ) )
        {
            logParameter( parameter );
        }
    }
}
