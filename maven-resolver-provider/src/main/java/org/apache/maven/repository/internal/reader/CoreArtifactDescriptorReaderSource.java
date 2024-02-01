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
package org.apache.maven.repository.internal.reader;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.*;

import org.apache.maven.model.*;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.sisu.Priority;

/**
 * The core source, that populates all elements Maven needs.
 *
 * @since TBD
 */
@Singleton
@Named(CoreArtifactDescriptorReaderSource.NAME)
@Priority(5)
public class CoreArtifactDescriptorReaderSource extends ArtifactDescriptorReaderSourceSupport {
    public static final String NAME = "core";

    @Override
    public void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

        for (Repository r : model.getRepositories()) {
            result.addRepository(ArtifactDescriptorUtils.toRemoteRepository(r));
        }

        for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
            result.addDependency(convert(dependency, stereotypes));
        }

        DependencyManagement mgmt = model.getDependencyManagement();
        if (mgmt != null) {
            for (org.apache.maven.model.Dependency dependency : mgmt.getDependencies()) {
                result.addManagedDependency(convert(dependency, stereotypes));
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();

        Prerequisites prerequisites = model.getPrerequisites();
        if (prerequisites != null) {
            properties.put("prerequisites.maven", prerequisites.getMaven());
        }

        List<License> licenses = model.getLicenses();
        properties.put("license.count", licenses.size());
        for (int i = 0; i < licenses.size(); i++) {
            License license = licenses.get(i);
            properties.put("license." + i + ".name", license.getName());
            properties.put("license." + i + ".url", license.getUrl());
            properties.put("license." + i + ".comments", license.getComments());
            properties.put("license." + i + ".distribution", license.getDistribution());
        }

        result.setProperties(properties);

        setArtifactProperties(result, model);
    }
}
