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
package org.apache.maven.internal.aether;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.spi.PropertyContributor;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.rtinfo.RuntimeInformation;

/**
 * Extender that manages {@link PropertyContributor}.
 *
 * @since 4.0.0
 */
@Named
@Singleton
class PropertyContributorExtender implements MavenExecutionRequestExtender {
    private final Lookup lookup;

    @Inject
    PropertyContributorExtender(Lookup lookup) {
        this.lookup = lookup;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void extend(MavenExecutionRequest mavenExecutionRequest) {
        Map<String, PropertyContributor> effectivePropertyContributors = lookup.lookupMap(PropertyContributor.class);
        if (!effectivePropertyContributors.isEmpty()) {
            final Version mavenVersion = lookup.lookup(VersionParser.class)
                    .parseVersion(lookup.lookup(RuntimeInformation.class).getMavenVersion());
            final Map<String, String> systemPropertiesMap =
                    Map.copyOf((Map) mavenExecutionRequest.getSystemProperties());
            final Map<String, String> userPropertiesMap = Map.copyOf((Map) mavenExecutionRequest.getUserProperties());
            final ProtoSession protoSession = new ProtoSession() {
                @Override
                public Version getMavenVersion() {
                    return mavenVersion;
                }

                @Override
                public Instant getStartTime() {
                    return mavenExecutionRequest.getStartTime().toInstant();
                }

                @Override
                public Map<String, String> getUserProperties() {
                    return userPropertiesMap;
                }

                @Override
                public Map<String, String> getSystemProperties() {
                    return systemPropertiesMap;
                }

                @Override
                public Path getTopDirectory() {
                    return mavenExecutionRequest.getTopDirectory();
                }

                @Override
                public Path getRootDirectory() {
                    return mavenExecutionRequest.getRootDirectory();
                }
            };

            final Properties newProperties = new Properties();

            for (PropertyContributor contributor : effectivePropertyContributors.values()) {
                Map<String, String> contribution = contributor.contribute(protoSession);
                if (contribution != null && !contribution.isEmpty()) {
                    newProperties.putAll(contribution);
                }
            }

            if (!newProperties.isEmpty()) {
                Properties newUserProperties = new Properties();
                newUserProperties.putAll(userPropertiesMap);
                newUserProperties.putAll(newProperties);
                mavenExecutionRequest.setUserProperties(newUserProperties);
            }
        }
    }
}
