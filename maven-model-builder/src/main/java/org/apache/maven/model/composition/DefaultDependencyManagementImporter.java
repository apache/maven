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
package org.apache.maven.model.composition;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.*;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;

/**
 * Handles the import of dependency management from other models into the target model.
 *
 */
@Named
@Singleton
public class DefaultDependencyManagementImporter implements DependencyManagementImporter {

    @Override
    public Model importManagement(
            Model target,
            List<? extends DependencyManagement> sources,
            ModelBuildingRequest request,
            ModelProblemCollector problems) {
        if (sources != null && !sources.isEmpty()) {
            Map<String, Dependency> dependencies = new LinkedHashMap<>();

            DependencyManagement depMgmt = target.getDependencyManagement();

            if (depMgmt != null) {
                for (Dependency dependency : depMgmt.getDependencies()) {
                    dependencies.put(dependency.getManagementKey(), dependency);
                }
            } else {
                depMgmt = DependencyManagement.newInstance();
            }

            Set<String> directDependencies = new HashSet<>(dependencies.keySet());

            for (DependencyManagement source : sources) {
                for (Dependency dependency : source.getDependencies()) {
                    String key = dependency.getManagementKey();
                    Dependency present = dependencies.putIfAbsent(key, dependency);
                    if (present != null && !equals(dependency, present) && !directDependencies.contains(key)) {
                        // TODO: https://issues.apache.org/jira/browse/MNG-8004
                        problems.add(new ModelProblemCollectorRequest(
                                        ModelProblem.Severity.WARNING, ModelProblem.Version.V40)
                                .setMessage("Ignored POM import for: " + toString(dependency) + " as already imported "
                                        + toString(present) + ". Add the conflicting managed dependency directly "
                                        + "to the dependencyManagement section of the POM."));
                    }

                    if (present == null && request.isLocationTracking()) {
                        Dependency updatedDependency = updateWithImportedFrom(dependency, source);
                        dependencies.put(key, updatedDependency);
                    }
                }
            }

            return target.withDependencyManagement(depMgmt.withDependencies(dependencies.values()));
        }
        return target;
    }

    private String toString(Dependency dependency) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append(dependency.getGroupId())
                .append(":")
                .append(dependency.getArtifactId())
                .append(":")
                .append(dependency.getType());
        if (dependency.getClassifier() != null && !dependency.getClassifier().isEmpty()) {
            stringBuilder.append(":").append(dependency.getClassifier());
        }
        stringBuilder
                .append(":")
                .append(dependency.getVersion())
                .append("@")
                .append(dependency.getScope() == null ? "compile" : dependency.getScope());
        if (dependency.isOptional()) {
            stringBuilder.append("[optional]");
        }
        if (!dependency.getExclusions().isEmpty()) {
            stringBuilder.append("[").append(dependency.getExclusions().size()).append(" exclusions]");
        }
        return stringBuilder.toString();
    }

    private boolean equals(Dependency d1, Dependency d2) {
        return Objects.equals(d1.getGroupId(), d2.getGroupId())
                && Objects.equals(d1.getArtifactId(), d2.getArtifactId())
                && Objects.equals(d1.getVersion(), d2.getVersion())
                && Objects.equals(d1.getType(), d2.getType())
                && Objects.equals(d1.getClassifier(), d2.getClassifier())
                && Objects.equals(d1.getScope(), d2.getScope())
                && Objects.equals(d1.getSystemPath(), d2.getSystemPath())
                && Objects.equals(d1.getOptional(), d2.getOptional())
                && equals(d1.getExclusions(), d2.getExclusions());
    }

    private boolean equals(Collection<Exclusion> ce1, Collection<Exclusion> ce2) {
        if (ce1.size() == ce2.size()) {
            Iterator<Exclusion> i1 = ce1.iterator();
            Iterator<Exclusion> i2 = ce2.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                if (!equals(i1.next(), i2.next())) {
                    return false;
                }
            }
            return !i1.hasNext() && !i2.hasNext();
        }
        return false;
    }

    private boolean equals(Exclusion e1, Exclusion e2) {
        return Objects.equals(e1.getGroupId(), e2.getGroupId())
                && Objects.equals(e1.getArtifactId(), e2.getArtifactId());
    }

    static Dependency updateWithImportedFrom(Dependency dependency, DependencyManagement bom) {
        // We are only interested in the InputSource, so the location of the <dependency> element is sufficient
        InputLocation dependencyLocation = dependency.getLocation("");
        InputLocation bomLocation = bom.getLocation("");

        if (dependencyLocation == null || bomLocation == null) {
            return dependency;
        }

        InputSource dependencySource = dependencyLocation.getSource();
        InputSource bomSource = bomLocation.getSource();

        // If the dependency and BOM have the same source, it means we found the root where the dependency is declared.
        if (dependencySource == null
                || bomSource == null
                || Objects.equals(dependencySource.getModelId(), bomSource.getModelId())) {
            return Dependency.newBuilder(dependency, true)
                    .importedFrom(bomLocation)
                    .build();
        }

        while (dependencySource.getImportedFrom() != null) {
            InputLocation importedFrom = dependencySource.getImportedFrom();

            // Stop if the BOM is already in the list, no update necessary
            if (Objects.equals(importedFrom.getSource().getModelId(), bomSource.getModelId())) {
                return dependency;
            }

            dependencySource = importedFrom.getSource();
        }

        // We modify the input location that is used for the whole file.
        // This is likely correct because the POM hierarchy applies to the whole POM, not just one dependency.
        return Dependency.newBuilder(dependency, true)
                .importedFrom(new InputLocation(bomLocation))
                .build();
    }
}
