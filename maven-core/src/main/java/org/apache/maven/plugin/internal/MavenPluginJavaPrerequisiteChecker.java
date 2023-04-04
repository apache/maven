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
package org.apache.maven.plugin.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MavenPluginPrerequisitesChecker;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

@Named
@Singleton
public class MavenPluginJavaPrerequisiteChecker implements MavenPluginPrerequisitesChecker {

    private final VersionScheme versionScheme;

    @Inject
    public MavenPluginJavaPrerequisiteChecker(final VersionScheme versionScheme) {
        this.versionScheme = versionScheme;
    }

    @Override
    public void accept(PluginDescriptor pluginDescriptor) {
        String requiredJavaVersion = pluginDescriptor.getRequiredJavaVersion();
        if (requiredJavaVersion != null && !requiredJavaVersion.isEmpty()) {
            String currentJavaVersion = System.getProperty("java.version");
            if (!matchesVersion(requiredJavaVersion, currentJavaVersion)) {
                throw new IllegalStateException("Required Java version " + requiredJavaVersion
                        + " is not met by current version: " + currentJavaVersion);
            }
        }
    }

    boolean matchesVersion(String requiredVersion, String currentVersion) {
        VersionConstraint constraint;
        try {
            constraint = versionScheme.parseVersionConstraint(requiredVersion);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException("Invalid 'requiredJavaVersion' given in plugin descriptor", e);
        }
        Version current;
        try {
            current = versionScheme.parseVersion(currentVersion);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException("Could not parse current Java version", e);
        }
        if (constraint.getRange() == null) {
            return constraint.getVersion().compareTo(current) <= 0;
        }
        return constraint.containsVersion(current);
    }
}
