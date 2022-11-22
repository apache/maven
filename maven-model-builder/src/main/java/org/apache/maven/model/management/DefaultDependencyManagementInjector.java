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
package org.apache.maven.model.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;

/**
 * Handles injection of dependency management into the model.
 *
 * @author Benjamin Bentmann
 */
@SuppressWarnings({"checkstyle:methodname"})
@Named
@Singleton
public class DefaultDependencyManagementInjector implements DependencyManagementInjector {

    private ManagementModelMerger merger = new ManagementModelMerger();

    @Override
    public void injectManagement(
            org.apache.maven.model.Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        model.update(merger.mergeManagedDependencies(model.getDelegate()));
    }

    /**
     * ManagementModelMerger
     */
    protected static class ManagementModelMerger extends MavenModelMerger {

        public Model mergeManagedDependencies(Model model) {
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if (dependencyManagement != null) {
                Map<Object, Dependency> dependencies = new HashMap<>();
                Map<Object, Object> context = Collections.emptyMap();

                for (Dependency dependency : model.getDependencies()) {
                    Object key = getDependencyKey().apply(dependency);
                    dependencies.put(key, dependency);
                }

                boolean modified = false;
                for (Dependency managedDependency : dependencyManagement.getDependencies()) {
                    Object key = getDependencyKey().apply(managedDependency);
                    Dependency dependency = dependencies.get(key);
                    if (dependency != null) {
                        Dependency merged = mergeDependency(dependency, managedDependency, false, context);
                        if (merged != dependency) {
                            dependencies.put(key, merged);
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    List<Dependency> newDeps = new ArrayList<>(dependencies.size());
                    for (Dependency dep : model.getDependencies()) {
                        Object key = getDependencyKey().apply(dep);
                        Dependency dependency = dependencies.get(key);
                        newDeps.add(dependency);
                    }
                    return Model.newBuilder(model).dependencies(newDeps).build();
                }
            }
            return model;
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
