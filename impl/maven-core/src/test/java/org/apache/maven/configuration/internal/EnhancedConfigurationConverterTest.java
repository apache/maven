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

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EnhancedConfigurationConverter}, focusing on instantiation of
 * classes with non-public constructors (fix for #13459).
 */
class EnhancedConfigurationConverterTest {

    private final EnhancedConfigurationConverter converter = new EnhancedConfigurationConverter();
    private final DefaultConverterLookup lookup = new DefaultConverterLookup();
    private final ExpressionEvaluator evaluator = mock(ExpressionEvaluator.class);
    private final ConfigurationListener listener = mock(ConfigurationListener.class);

    @AfterEach
    void tearDown() {
        EnhancedConfigurationConverter.clearCaches();
    }

    /**
     * Regression test for #13459: a class with a package-private no-arg constructor must be
     * instantiable via {@code fromConfiguration()}. Before the fix, the inherited
     * {@code instantiateObject()} used {@code Class.newInstance()} which requires a public
     * constructor and would throw {@link IllegalAccessException} for package-private classes.
     */
    @Test
    void testFromConfigurationWithPackagePrivateConstructor() throws Exception {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration("config");
        config.addChild("value", "hello");

        Object result = converter.fromConfiguration(
                lookup, config, PackagePrivateBean.class, null, getClass().getClassLoader(), evaluator, listener);

        assertNotNull(result, "fromConfiguration must return a non-null instance");
        assertInstanceOf(PackagePrivateBean.class, result, "Result must be a PackagePrivateBean");
    }

    /**
     * Verify that a class with no no-arg constructor at all still throws
     * {@link ComponentConfigurationException} (not an unrelated reflection error).
     */
    @Test
    void testFromConfigurationWithNoDefaultConstructorThrows() {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration("config");

        assertThrows(
                ComponentConfigurationException.class,
                () -> converter.fromConfiguration(
                        lookup,
                        config,
                        NoDefaultConstructorBean.class,
                        null,
                        getClass().getClassLoader(),
                        evaluator,
                        listener));
    }

    // -------------------------------------------------------------------------
    // Helper types — same package so the package-private constructor is valid
    // -------------------------------------------------------------------------

    /** A bean with a package-private no-arg constructor: the exact scenario fixed by #13459. */
    static class PackagePrivateBean {
        String value;

        // package-private constructor — inaccessible via Class.newInstance()
        PackagePrivateBean() {}
    }

    /** A bean with no no-arg constructor at all. */
    static class NoDefaultConstructorBean {
        @SuppressWarnings("unused")
        NoDefaultConstructorBean(String required) {}
    }
}
