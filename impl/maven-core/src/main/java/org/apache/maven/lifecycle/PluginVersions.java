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
package org.apache.maven.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides default plugin versions for the built-in lifecycle bindings.
 * <p>
 * Versions are read from {@code plugin-versions.properties}, which is filtered
 * at build time from POM properties ({@code version.maven-<name>-plugin}).
 * Centralising them in the POM makes them visible to dependency-update bots
 * such as Dependabot and Renovate.
 *
 * @since 4.1.0
 */
public final class PluginVersions {

    private static final Properties VERSIONS = new Properties();

    static {
        try (InputStream in = PluginVersions.class.getResourceAsStream("plugin-versions.properties")) {
            if (in == null) {
                throw new ExceptionInInitializerError("plugin-versions.properties not found on classpath");
            }
            VERSIONS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PluginVersions() {}

    /**
     * Returns the default version for the given plugin.
     *
     * @param pluginArtifactId the artifact id, e.g. {@code "maven-compiler-plugin"}
     * @return the version string, never {@code null}
     * @throws IllegalArgumentException if the plugin is not listed in the properties file
     */
    public static String version(String pluginArtifactId) {
        String key = pluginArtifactId + ".version";
        String version = VERSIONS.getProperty(key);
        if (version == null) {
            throw new IllegalArgumentException("No default version defined for " + pluginArtifactId + "; add " + key
                    + " to plugin-versions.properties");
        }
        return version;
    }

    // --- convenience constants used by lifecycle mapping providers ---

    public static final String CLEAN = version("maven-clean-plugin");
    public static final String COMPILER = version("maven-compiler-plugin");
    public static final String DEPLOY = version("maven-deploy-plugin");
    public static final String EAR = version("maven-ear-plugin");
    public static final String EJB = version("maven-ejb-plugin");
    public static final String INSTALL = version("maven-install-plugin");
    public static final String JAR = version("maven-jar-plugin");
    public static final String PLUGIN = version("maven-plugin-plugin");
    public static final String RAR = version("maven-rar-plugin");
    public static final String RESOURCES = version("maven-resources-plugin");
    public static final String SITE = version("maven-site-plugin");
    public static final String SUREFIRE = version("maven-surefire-plugin");
    public static final String WAR = version("maven-war-plugin");
}
