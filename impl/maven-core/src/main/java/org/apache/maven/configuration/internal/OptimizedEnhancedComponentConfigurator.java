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
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.converters.special.ClassRealmConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Optimized version of EnhancedComponentConfigurator that uses caching for better performance.
 * This configurator is specifically designed to improve mojo configuration performance.
 */
@Named("optimized")
@Singleton
public class OptimizedEnhancedComponentConfigurator extends EnhancedComponentConfigurator {

    private final ConverterLookup converterLookup = new DefaultConverterLookup();

    @Override
    public void configureComponent(
            final Object component,
            final PlexusConfiguration configuration,
            final ExpressionEvaluator evaluator,
            final ClassRealm realm,
            final ConfigurationListener listener)
            throws ComponentConfigurationException {
        try {
            ClassRealmConverter.pushContextRealm(realm);
            this.configureComponent(component, configuration, evaluator, (ClassLoader) realm, listener);
        } finally {
            ClassRealmConverter.popContextRealm();
        }
    }

    @Override
    public void configureComponent(
            Object component,
            PlexusConfiguration configuration,
            ExpressionEvaluator evaluator,
            ClassLoader loader,
            ConfigurationListener listener)
            throws ComponentConfigurationException {

        // Use our optimized configuration converter for better performance
        new OptimizedEnhancedConfigurationConverter()
                .processConfiguration(converterLookup, component, loader, configuration, evaluator, listener);
    }
}
