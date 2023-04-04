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
package org.apache.maven.rtinfo.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides information about the current Maven runtime.
 */
@Named
@Singleton
public class DefaultRuntimeInformation implements RuntimeInformation {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VersionScheme versionScheme;

    private final String mavenVersion;

    @Inject
    public DefaultRuntimeInformation(final VersionScheme versionScheme) {
        this.versionScheme = versionScheme;
        this.mavenVersion = loadMavenVersion();
    }

    @Override
    public String getMavenVersion() {
        return mavenVersion;
    }

    private String loadMavenVersion() {
        Properties props = new Properties();

        String resource = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

        try (InputStream is = DefaultRuntimeInformation.class.getResourceAsStream("/" + resource)) {
            if (is != null) {
                props.load(is);
            } else {
                logger.warn("Could not locate " + resource + " on classpath, Maven runtime information not available");
            }
        } catch (IOException e) {
            String msg = "Could not parse " + resource + ", Maven runtime information not available";
            if (logger.isDebugEnabled()) {
                logger.warn(msg, e);
            } else {
                logger.warn(msg);
            }
        }

        String version = props.getProperty("version", "").trim();

        if (!version.startsWith("${")) {
            return version;
        } else {
            return "";
        }
    }

    @Override
    public boolean isMavenVersion(String versionRange) {
        if (Objects.requireNonNull(versionRange, "versionRange cannot be null").isEmpty()) {
            throw new IllegalArgumentException("versionRange cannot be empty");
        }

        VersionConstraint constraint;
        try {
            constraint = versionScheme.parseVersionConstraint(versionRange);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        Version current;
        try {
            String mavenVersion = getMavenVersion();
            if (mavenVersion.isEmpty()) {
                throw new IllegalArgumentException("Could not determine current Maven version");
            }

            current = versionScheme.parseVersion(mavenVersion);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException("Could not parse current Maven version: " + e.getMessage(), e);
        }

        if (constraint.getRange() == null) {
            return constraint.getVersion().compareTo(current) <= 0;
        }
        return constraint.containsVersion(current);
    }
}
