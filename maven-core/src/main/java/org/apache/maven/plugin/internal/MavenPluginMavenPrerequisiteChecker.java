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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.plugin.MavenPluginPrerequisitesChecker;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.spi.version.VersionSchemeSelector;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MavenPluginMavenPrerequisiteChecker implements MavenPluginPrerequisitesChecker {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RuntimeInformation runtimeInformation;
    private final Provider<InternalSession> internalSessionProvider;
    private final VersionSchemeSelector versionSchemeSelector;

    @Inject
    public MavenPluginMavenPrerequisiteChecker(
            RuntimeInformation runtimeInformation,
            Provider<InternalSession> internalSessionProvider,
            VersionSchemeSelector versionSchemeSelector) {
        this.runtimeInformation = runtimeInformation;
        this.internalSessionProvider = internalSessionProvider;
        this.versionSchemeSelector = versionSchemeSelector;
    }

    @Override
    public void accept(PluginDescriptor pluginDescriptor) {
        String requiredMavenVersion = pluginDescriptor.getRequiredMavenVersion();

        boolean isBlankVersion =
                requiredMavenVersion == null || requiredMavenVersion.trim().isEmpty();

        if (!isBlankVersion) {
            VersionScheme versionScheme = versionSchemeSelector.selectVersionScheme(
                    internalSessionProvider.get().getSession()); // this must happen within session
            VersionConstraint constraint;
            try {
                constraint = versionScheme.parseVersionConstraint(requiredMavenVersion);
            } catch (InvalidVersionSpecificationException e) {
                logger.warn(
                        "Could not verify plugin's Maven prerequisite as an invalid version is given in "
                                + requiredMavenVersion,
                        e);
                return;
            }

            Version current;
            try {
                String mavenVersion = runtimeInformation.getMavenVersion();
                if (mavenVersion == null || mavenVersion.isEmpty()) {
                    throw new IllegalArgumentException("Could not determine current Maven version");
                }
                current = versionScheme.parseVersion(mavenVersion);
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalStateException("Could not parse current Maven version: " + e.getMessage(), e);
            }

            boolean isRequirementMet;
            if (constraint.getRange() == null) {
                isRequirementMet = constraint.getVersion().compareTo(current) <= 0;
            } else {
                isRequirementMet = constraint.containsVersion(current);
            }
            if (!isRequirementMet) {
                throw new IllegalStateException("Required Maven version " + requiredMavenVersion
                        + " is not met by current version " + runtimeInformation.getMavenVersion());
            }
        }
    }
}
