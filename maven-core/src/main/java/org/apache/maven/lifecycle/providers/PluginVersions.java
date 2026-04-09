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
package org.apache.maven.lifecycle.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Provider of plugin versions.
 *
 * @since 3.10.0
 */
public final class PluginVersions {
    private PluginVersions() {}

    // basic
    public static final String RESOURCES_PLUGIN_VERSION;
    public static final String COMPILER_PLUGIN_VERSION;
    public static final String SUREFIRE_PLUGIN_VERSION;
    public static final String INSTALL_PLUGIN_VERSION;
    public static final String DEPLOY_PLUGIN_VERSION;
    // packaging
    public static final String JAR_PLUGIN_VERSION;
    public static final String EAR_PLUGIN_VERSION;
    public static final String EJB_PLUGIN_VERSION;
    public static final String PLUGIN_PLUGIN_VERSION;
    public static final String RAR_PLUGIN_VERSION;
    public static final String WAR_PLUGIN_VERSION;
    // lifecycles
    public static final String CLEAN_PLUGIN_VERSION;
    public static final String SITE_PLUGIN_VERSION;

    static {
        Properties properties = new Properties();
        try {
            try (InputStream inputStream = PluginVersions.class
                    .getClassLoader()
                    .getResourceAsStream("org/apache/maven/lifecycle/providers/plugin-versions.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            }
            RESOURCES_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-resources-plugin"));
            COMPILER_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-compiler-plugin"));
            SUREFIRE_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-surefire-plugin"));
            INSTALL_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-install-plugin"));
            DEPLOY_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-deploy-plugin"));
            JAR_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-jar-plugin"));
            EAR_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-ear-plugin"));
            EJB_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-ejb-plugin"));
            PLUGIN_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-plugin-plugin"));
            RAR_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-rar-plugin"));
            WAR_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-war-plugin"));
            CLEAN_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-clean-plugin"));
            SITE_PLUGIN_VERSION = requireNonNull(properties.getProperty("maven-site-plugin"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load plugin versions", e);
        }
    }
}
