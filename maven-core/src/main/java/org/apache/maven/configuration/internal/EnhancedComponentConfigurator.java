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
package org.apache.maven.configuration.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.special.ClassRealmConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * A component configurator which can leverage the {@link EnhancedConfigurationConverter} and
 * {@link EnhancedConverterLookup}.
 */
@Singleton
@Named( "enhanced" )
public class EnhancedComponentConfigurator
    extends BasicComponentConfigurator
{

    public EnhancedComponentConfigurator()
    {
        converterLookup = new EnhancedConverterLookup();
    }

    @Override
    public void configureComponent( final Object component, final PlexusConfiguration configuration,
                                    final ExpressionEvaluator evaluator, final ClassRealm realm,
                                    final ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        try
        {
            ClassRealmConverter.pushContextRealm( realm );

            new EnhancedConfigurationConverter().processConfiguration( converterLookup, component, realm, //
                                                                       configuration, evaluator, listener );
        }
        finally
        {
            ClassRealmConverter.popContextRealm();
        }
    }

}
