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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.Mixin;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ModelNormalizer;

/**
 * Handles normalization of a model.
 *
 */
@Named
@Singleton
public class DefaultModelNormalizer implements ModelNormalizer {

    private DuplicateMerger merger = new DuplicateMerger();

    @Override
    public Model mergeDuplicates(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model.Builder builder = Model.newBuilder(model);

        // Expand id attributes on mixins
        builder.mixins(injectList(model.getMixins(), this::expandMixinGav));

        Build build = model.getBuild();
        if (build != null) {
            List<Plugin> plugins = build.getPlugins();
            Map<Object, Plugin> normalized = new LinkedHashMap<>(plugins.size() * 2);

            for (Plugin plugin : plugins) {
                Object key = plugin.getKey();
                Plugin first = normalized.get(key);
                if (first != null) {
                    plugin = merger.mergePlugin(plugin, first);
                }
                normalized.put(key, plugin);
            }

            if (plugins.size() != normalized.size()) {
                builder.build(
                        Build.newBuilder(build).plugins(normalized.values()).build());
            }
        }

        /*
         * NOTE: This is primarily to keep backward-compat with Maven 2.x which did not validate that dependencies are
         * unique within a single POM. Upon multiple declarations, 2.x just kept the last one but retained the order of
         * the first occurrence. So when we're in lenient/compat mode, we have to deal with such broken POMs and mimic
         * the way 2.x works. When we're in strict mode, the removal of duplicates just saves other merging steps from
         * aftereffects and bogus error messages.
         */
        // Expand id attributes on dependencies (and their exclusions), then deduplicate
        builder.dependencies(expandAndDeduplicateDependencies(model.getDependencies()));

        // Expand id attributes on dependency management dependencies
        DependencyManagement mgmt = model.getDependencyManagement();
        if (mgmt != null) {
            List<Dependency> expandedMgmt = expandAndDeduplicateDependencies(mgmt.getDependencies());
            if (expandedMgmt != null) {
                builder.dependencyManagement(DependencyManagement.newBuilder(mgmt)
                        .dependencies(expandedMgmt)
                        .build());
            }
        }

        // Expand id attributes in profile dependencies and dependency management
        List<Profile> profiles = injectList(model.getProfiles(), this::expandProfileDependencyIds);
        if (profiles != null) {
            builder.profiles(profiles);
        }

        return builder.build();
    }

    /**
     * DuplicateMerger
     */
    protected static class DuplicateMerger extends MavenModelMerger {

        public Plugin mergePlugin(Plugin target, Plugin source) {
            return super.mergePlugin(target, source, false, Collections.emptyMap());
        }
    }

    @Override
    public Model injectDefaultValues(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model.Builder builder = Model.newBuilder(model);

        builder.dependencies(injectList(model.getDependencies(), this::injectDependency));
        Build build = model.getBuild();
        if (build != null) {
            Build newBuild = Build.newBuilder(build)
                    .plugins(injectList(build.getPlugins(), this::injectPlugin))
                    .build();
            builder.build(newBuild != build ? newBuild : null);
        }

        return builder.build();
    }

    private Plugin injectPlugin(Plugin p) {
        return Plugin.newBuilder(p)
                .dependencies(injectList(p.getDependencies(), this::injectDependency))
                .build();
    }

    private Dependency injectDependency(Dependency d) {
        // we cannot set this directly in the MDO due to the interactions with dependency management
        return (d.getScope() == null || d.getScope().isEmpty()) ? d.withScope("compile") : d;
    }

    /**
     * Returns a list suited for the builders, i.e. null if not modified
     */
    private <T> List<T> injectList(List<T> list, UnaryOperator<T> modifer) {
        List<T> newList = null;
        for (int i = 0; i < list.size(); i++) {
            T oldT = list.get(i);
            T newT = modifer.apply(oldT);
            if (newT != oldT) {
                if (newList == null) {
                    newList = new ArrayList<>(list);
                }
                newList.set(i, newT);
            }
        }
        return newList;
    }

    private List<Dependency> expandAndDeduplicateDependencies(List<Dependency> dependencies) {
        List<Dependency> expanded = injectList(dependencies, this::expandDependencyId);
        if (expanded != null) {
            dependencies = expanded;
        }
        Map<String, Dependency> normalized = new LinkedHashMap<>(dependencies.size() * 2);
        for (Dependency dependency : dependencies) {
            normalized.put(dependency.getManagementKey(), dependency);
        }
        if (expanded != null || dependencies.size() != normalized.size()) {
            return new ArrayList<>(normalized.values());
        }
        return null;
    }

    private Profile expandProfileDependencyIds(Profile profile) {
        Profile.Builder pb = null;

        List<Dependency> deps = expandAndDeduplicateDependencies(profile.getDependencies());
        if (deps != null) {
            pb = Profile.newBuilder(profile);
            pb.dependencies(deps);
        }

        DependencyManagement mgmt = profile.getDependencyManagement();
        if (mgmt != null) {
            List<Dependency> mgmtDeps = expandAndDeduplicateDependencies(mgmt.getDependencies());
            if (mgmtDeps != null) {
                if (pb == null) {
                    pb = Profile.newBuilder(profile);
                }
                pb.dependencyManagement(DependencyManagement.newBuilder(mgmt)
                        .dependencies(mgmtDeps)
                        .build());
            }
        }

        return pb != null ? pb.build() : profile;
    }

    /**
     * Expands the {@code id} attribute on a dependency into its component fields.
     * The id format is {@code groupId:artifactId:version[@scope][?]}.
     */
    Dependency expandDependencyId(Dependency d) {
        String id = d.getId();
        if (id == null || id.isEmpty()) {
            // No id attribute, but still expand exclusion ids
            List<Exclusion> expanded = injectList(d.getExclusions(), this::expandExclusionId);
            return expanded != null ? d.withExclusions(expanded) : d;
        }

        String remaining = id;
        boolean optional = false;
        if (remaining.endsWith("?")) {
            optional = true;
            remaining = remaining.substring(0, remaining.length() - 1);
        }

        String scope = null;
        int atIndex = remaining.lastIndexOf('@');
        if (atIndex >= 0) {
            scope = remaining.substring(atIndex + 1);
            remaining = remaining.substring(0, atIndex);
        }

        String[] parts = remaining.split(":");
        if (parts.length != 3) {
            // Invalid format — will be caught by the validator
            return d;
        }
        Dependency.Builder builder = Dependency.newBuilder(d);
        if (isNullOrEmpty(d.getGroupId())) {
            builder.groupId(parts[0]);
        }
        if (isNullOrEmpty(d.getArtifactId())) {
            builder.artifactId(parts[1]);
        }
        if (isNullOrEmpty(d.getVersion())) {
            builder.version(parts[2]);
        }
        if (scope != null && isNullOrEmpty(d.getScope())) {
            builder.scope(scope);
        }
        if (optional && isNullOrEmpty(d.getOptional())) {
            builder.optional("true");
        }
        List<Exclusion> expanded = injectList(d.getExclusions(), this::expandExclusionId);
        if (expanded != null) {
            builder.exclusions(expanded);
        }
        return builder.build();
    }

    /**
     * Expands the {@code id} attribute on an exclusion into its component fields.
     * The id format is {@code groupId:artifactId}.
     */
    Exclusion expandExclusionId(Exclusion e) {
        String id = e.getId();
        if (id == null || id.isEmpty()) {
            return e;
        }
        String[] parts = id.split(":");
        if (parts.length != 2) {
            // Invalid format — will be caught by the validator
            return e;
        }
        Exclusion.Builder builder = Exclusion.newBuilder(e);
        if (isNullOrEmpty(e.getGroupId())) {
            builder.groupId(parts[0]);
        }
        if (isNullOrEmpty(e.getArtifactId())) {
            builder.artifactId(parts[1]);
        }
        return builder.build();
    }

    /**
     * Expands the {@code id} (XML attribute) / {@code gav} (Java field) on a mixin
     * into its component fields. The format is {@code groupId:artifactId:version}.
     */
    Mixin expandMixinGav(Mixin m) {
        String gav = m.getGav();
        if (gav == null || gav.isEmpty()) {
            return m;
        }
        String[] parts = gav.split(":");
        if (parts.length != 3) {
            // Invalid format — will be caught by the validator
            return m;
        }
        Mixin.Builder builder = Mixin.newBuilder(m);
        if (isNullOrEmpty(m.getGroupId())) {
            builder.groupId(parts[0]);
        }
        if (isNullOrEmpty(m.getArtifactId())) {
            builder.artifactId(parts[1]);
        }
        if (isNullOrEmpty(m.getVersion())) {
            builder.version(parts[2]);
        }
        return builder.build();
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
