package org.apache.maven.caching;

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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.AbstractComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static java.util.Objects.requireNonNull;

/**
 * A wagon configurator based on the Plexus component configuration framework.
 */
@Singleton
public class WagonConfigurator
{
    private final PlexusContainer container;

    /**
     * Creates a wagon configurator using the specified Plexus container.
     *
     * @param container The Plexus container instance to use, must not be {@code null}.
     */
    @Inject
    public WagonConfigurator( final PlexusContainer container )
    {
        this.container = requireNonNull( container, "plexus container cannot be null" );
    }

    public void configure( Wagon wagon, Object configuration )
        throws Exception
    {
        requireNonNull( wagon, "wagon cannot be null" );
        requireNonNull( configuration, "configuration cannot be null" );

        PlexusConfiguration config;
        if ( configuration instanceof PlexusConfiguration )
        {
            config = (PlexusConfiguration) configuration;
        }
        else if ( configuration instanceof Xpp3Dom )
        {
            config = new XmlPlexusConfiguration( (Xpp3Dom) configuration );
        }
        else
        {
            throw new IllegalArgumentException( "unexpected configuration type: "
                    + configuration.getClass().getName() );
        }

        WagonComponentConfigurator configurator = new WagonComponentConfigurator();

        configurator.configureComponent( wagon, config, container.getContainerRealm() );
    }

    static class WagonComponentConfigurator
        extends AbstractComponentConfigurator
    {

        @Override
        public void configureComponent( Object component, PlexusConfiguration configuration,
                                        ExpressionEvaluator expressionEvaluator, ClassRealm containerRealm,
                                        ConfigurationListener listener )
            throws ComponentConfigurationException
        {
            ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();

            converter.processConfiguration( converterLookup, component, containerRealm, configuration,
                                            expressionEvaluator, listener );
        }

    }

}
