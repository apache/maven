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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.DependencyManagementInjector;

/**
 * Handles injection of dependency management into the model.
 *
 */
@SuppressWarnings({"checkstyle:methodname"})
@Named
@Singleton
public class DefaultDependencyManagementInjector implements DependencyManagementInjector {

    private ManagementModelMerger merger = new ManagementModelMerger();

    @Override
    public void injectManagement(Model.Builder builder, ModelBuilderRequest request, ModelProblemCollector problems) {
        // Use builder getters instead of builder.build() to avoid materializing
        // all model-object lists just to read Dependencies and DependencyManagement
        DependencyManagement depMgmt = builder.getDependencyManagement();
        if (depMgmt != null) {
            List<Dependency> deps = builder.getBuiltDependencies();
            List<Dependency> merged = merger.computeMergedDependencies(deps, depMgmt);
            if (merged != null) {
                builder.dependencies(merged);
            }
        }
    }

    @Override
    public Model injectManagement(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        return merger.mergeManagedDependencies(model);
    }

    /**
     * ManagementModelMerger
     */
    protected static class ManagementModelMerger extends MavenModelMerger {

        /**
         * Computes the merged dependency list, or returns {@code null} if no dependencies were modified.
         */
        List<Dependency> computeMergedDependencies(Model model) {
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if (dependencyManagement != null) {
                return computeMergedDependencies(model.getDependencies(), dependencyManagement);
            }
            return null;
        }

        /**
         * Computes the merged dependency list from pre-extracted deps and dep management,
         * or returns {@code null} if no dependencies were modified.
         */
        List<Dependency> computeMergedDependencies(
                List<Dependency> dependencies, DependencyManagement dependencyManagement) {
            Map<Object, Dependency> originalDeps = new HashMap<>();
            Map<Object, Dependency.Builder> builderDeps = new HashMap<>();
            Map<Object, Object> context = Collections.emptyMap();

            for (Dependency dependency : dependencies) {
                Object key = getDependencyKey().apply(dependency);
                originalDeps.put(key, dependency);
            }

            boolean modified = false;
            for (Dependency managedDependency : dependencyManagement.getDependencies()) {
                Object key = getDependencyKey().apply(managedDependency);
                Dependency dependency = originalDeps.get(key);
                if (dependency != null) {
                    Dependency.Builder merged = mergeDependencyToBuilder(dependency, managedDependency, false, context);
                    if (merged != null) {
                        builderDeps.put(key, merged);
                        modified = true;
                    }
                }
            }

            if (modified) {
                List<Dependency> newDeps = new ArrayList<>(originalDeps.size());
                for (Dependency dep : dependencies) {
                    Object key = getDependencyKey().apply(dep);
                    Dependency.Builder builder = builderDeps.get(key);
                    newDeps.add(builder != null ? builder.build() : dep);
                }
                return newDeps;
            }
            return null;
        }

        public Model mergeManagedDependencies(Model model) {
            List<Dependency> merged = computeMergedDependencies(model);
            return merged != null ? Model.newBuilder(model).dependencies(merged).build() : model;
        }

        @Override
        protected void mergeDependency_Optional(
                Dependency.Builder builder,
                Dependency target,
                Dependency source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            // optional flag is not managed
        }

        @Override
        protected void mergeDependency_Exclusions(
                Dependency.Builder builder,
                Dependency target,
                Dependency source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<Exclusion> tgt = target.getExclusions();
            if (tgt.isEmpty()) {
                List<Exclusion> src = source.getExclusions();
                builder.exclusions(src);
            }
        }
    }
}
