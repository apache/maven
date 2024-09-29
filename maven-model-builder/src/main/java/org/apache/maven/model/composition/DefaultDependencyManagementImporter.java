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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;

/**
 * Handles the import of dependency management from other models into the target model.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
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
}
