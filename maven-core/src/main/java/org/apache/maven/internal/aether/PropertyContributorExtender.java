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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.spi.PropertyContributor;
import org.apache.maven.execution.MavenExecutionRequest;

/**
 * Extender that manages {@link PropertyContributor}.
 *
 * @since 4.0.0
 */
@Named
@Singleton
class PropertyContributorExtender implements MavenExecutionRequestExtender {
    private final Map<String, PropertyContributor> effectivePropertyContributors;

    @Inject
    PropertyContributorExtender(Map<String, PropertyContributor> effectivePropertyContributors) {
        this.effectivePropertyContributors = effectivePropertyContributors;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void extend(MavenExecutionRequest mavenExecutionRequest) {
        HashMap<String, String> userPropertiesMap = new HashMap<>((Map) mavenExecutionRequest.getUserProperties());
        for (PropertyContributor contributor : effectivePropertyContributors.values()) {
            contributor.contribute(userPropertiesMap);
        }
        Properties newProperties = new Properties();
        newProperties.putAll(userPropertiesMap);
        mavenExecutionRequest.setUserProperties(newProperties);
    }
}
