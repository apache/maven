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
package org.apache.maven.cli.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.cli.logging.impl.UnsupportedSlf4jBindingConfiguration;
import org.slf4j.ILoggerFactory;

/**
 * Slf4jConfiguration factory, loading implementations from <code>META-INF/maven/slf4j-configuration.properties</code>
 * configuration files in class loader: key is the class name of the ILoggerFactory, value is the class name of
 * the corresponding Slf4jConfiguration.
 *
 * @since 3.1.0
 */
public class Slf4jConfigurationFactory {
    public static final String RESOURCE = "META-INF/maven/slf4j-configuration.properties";

    public static Slf4jConfiguration getConfiguration(ILoggerFactory loggerFactory) {
        String slf4jBinding = loggerFactory.getClass().getCanonicalName();

        try {
            Enumeration<URL> resources =
                    Slf4jConfigurationFactory.class.getClassLoader().getResources(RESOURCE);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try {
                    InputStream is = resource.openStream();
                    final Properties properties = new Properties();
                    if (is != null) {
                        try (InputStream in = is) {
                            properties.load(in);
                        }
                    }
                    String impl = properties.getProperty(slf4jBinding);
                    if (impl != null) {
                        return (Slf4jConfiguration) Class.forName(impl).newInstance();
                    }
                } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                    // ignore and move on to the next
                }
            }
        } catch (IOException ex) {
            // ignore
        }

        return new UnsupportedSlf4jBindingConfiguration();
    }
}
