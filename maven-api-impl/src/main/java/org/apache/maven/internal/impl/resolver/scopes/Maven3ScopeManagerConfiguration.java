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
package org.apache.maven.internal.impl.resolver.scopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.impl.scope.BuildScopeMatrixSource;
import org.eclipse.aether.impl.scope.BuildScopeSource;
import org.eclipse.aether.impl.scope.CommonBuilds;
import org.eclipse.aether.impl.scope.InternalScopeManager;
import org.eclipse.aether.impl.scope.ScopeManagerConfiguration;
import org.eclipse.aether.internal.impl.scope.ScopeManagerDump;
import org.eclipse.aether.scope.DependencyScope;
import org.eclipse.aether.scope.ResolutionScope;

import static org.eclipse.aether.impl.scope.BuildScopeQuery.all;
import static org.eclipse.aether.impl.scope.BuildScopeQuery.byBuildPath;
import static org.eclipse.aether.impl.scope.BuildScopeQuery.byProjectPath;
import static org.eclipse.aether.impl.scope.BuildScopeQuery.select;
import static org.eclipse.aether.impl.scope.BuildScopeQuery.singleton;
import static org.eclipse.aether.impl.scope.BuildScopeQuery.union;

/**
 * Maven3 scope configurations. Configures scope manager to support Maven3 scopes.
 * <p>
 * This manager supports the old Maven 3 dependency scopes.
 *
 * @since 2.0.0
 */
public final class Maven3ScopeManagerConfiguration implements ScopeManagerConfiguration {
    public static final Maven3ScopeManagerConfiguration INSTANCE = new Maven3ScopeManagerConfiguration();
    public static final String DS_COMPILE = "compile";
    public static final String DS_RUNTIME = "runtime";
    public static final String DS_PROVIDED = "provided";
    public static final String DS_SYSTEM = "system";
    public static final String DS_TEST = "test";
    public static final String RS_NONE = "none";
    public static final String RS_MAIN_COMPILE = "main-compile";
    public static final String RS_MAIN_COMPILE_PLUS_RUNTIME = "main-compilePlusRuntime";
    public static final String RS_MAIN_RUNTIME = "main-runtime";
    public static final String RS_MAIN_RUNTIME_PLUS_SYSTEM = "main-runtimePlusSystem";
    public static final String RS_TEST_COMPILE = "test-compile";
    public static final String RS_TEST_RUNTIME = "test-runtime";

    private Maven3ScopeManagerConfiguration() {}

    @Override
    public String getId() {
        return "Maven3";
    }

    @Override
    public boolean isStrictDependencyScopes() {
        return false;
    }

    @Override
    public boolean isStrictResolutionScopes() {
        return false;
    }

    @Override
    public BuildScopeSource getBuildScopeSource() {
        return new BuildScopeMatrixSource(
                Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                CommonBuilds.MAVEN_TEST_BUILD_SCOPE);
    }

    @Override
    public Collection<DependencyScope> buildDependencyScopes(InternalScopeManager internalScopeManager) {
        ArrayList<DependencyScope> result = new ArrayList<>();
        result.add(internalScopeManager.createDependencyScope(DS_COMPILE, true, all()));
        result.add(internalScopeManager.createDependencyScope(
                DS_RUNTIME, true, byBuildPath(CommonBuilds.BUILD_PATH_RUNTIME)));
        result.add(internalScopeManager.createDependencyScope(
                DS_PROVIDED,
                false,
                union(
                        byBuildPath(CommonBuilds.BUILD_PATH_COMPILE),
                        select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME))));
        result.add(internalScopeManager.createDependencyScope(
                DS_TEST, false, byProjectPath(CommonBuilds.PROJECT_PATH_TEST)));
        result.add(internalScopeManager.createSystemDependencyScope(
                DS_SYSTEM, false, all(), ArtifactProperties.LOCAL_PATH));
        return result;
    }

    @Override
    public Collection<ResolutionScope> buildResolutionScopes(InternalScopeManager internalScopeManager) {
        Collection<DependencyScope> allDependencyScopes = internalScopeManager.getDependencyScopeUniverse();
        Collection<DependencyScope> nonTransitiveDependencyScopes =
                allDependencyScopes.stream().filter(s -> !s.isTransitive()).collect(Collectors.toSet());
        DependencyScope system =
                internalScopeManager.getDependencyScope(DS_SYSTEM).orElse(null);

        ArrayList<ResolutionScope> result = new ArrayList<>();
        result.add(internalScopeManager.createResolutionScope(
                RS_NONE,
                InternalScopeManager.Mode.REMOVE,
                Collections.emptySet(),
                Collections.emptySet(),
                allDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_COMPILE,
                InternalScopeManager.Mode.ELIMINATE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_COMPILE_PLUS_RUNTIME,
                InternalScopeManager.Mode.ELIMINATE,
                byProjectPath(CommonBuilds.PROJECT_PATH_MAIN),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_RUNTIME,
                InternalScopeManager.Mode.ELIMINATE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.emptySet(),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_RUNTIME_PLUS_SYSTEM,
                InternalScopeManager.Mode.ELIMINATE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_TEST_COMPILE,
                InternalScopeManager.Mode.ELIMINATE,
                select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_TEST_RUNTIME,
                InternalScopeManager.Mode.ELIMINATE,
                select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        return result;
    }

    // ===

    public static void main(String... args) {
        ScopeManagerDump.dump(Maven3ScopeManagerConfiguration.INSTANCE);
    }
}
