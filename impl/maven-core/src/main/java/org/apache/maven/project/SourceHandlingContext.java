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
package org.apache.maven.project;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.impl.model.DefaultModelProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles source configuration for Maven projects with unified tracking for all language/scope combinations.
 * <p>
 * This class replaces the previous approach of hardcoded boolean flags (hasMain, hasTest, etc.)
 * with a flexible set-based tracking mechanism that works for any language and scope combination.
 * <p>
 * Key features:
 * <ul>
 *   <li>Tracks declared sources using {@code (language, scope, module, directory)} identity</li>
 *   <li>Only tracks enabled sources - disabled sources are effectively no-ops</li>
 *   <li>Detects duplicate enabled sources and emits warnings</li>
 *   <li>Provides {@link #hasSources(Language, ProjectScope)} to check if sources exist for a combination</li>
 * </ul>
 *
 * @since 4.0.0
 */
class SourceHandlingContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceHandlingContext.class);

    /**
     * Identity key for source tracking. Two sources with the same key are considered duplicates.
     */
    record SourceKey(Language language, ProjectScope scope, String module, Path directory) {}

    private final MavenProject project;
    private final Path baseDir;
    private final Set<String> modules;
    private final boolean modularProject;
    private final ModelBuilderResult result;
    private final Set<SourceKey> declaredSources;

    SourceHandlingContext(
            MavenProject project,
            Path baseDir,
            Set<String> modules,
            boolean modularProject,
            ModelBuilderResult result) {
        this.project = project;
        this.baseDir = baseDir;
        this.modules = modules;
        this.modularProject = modularProject;
        this.result = result;
        // Each module typically has main, test, main resources, test resources = 4 sources
        this.declaredSources = new HashSet<>(4 * modules.size());
    }

    /**
     * Determines if a source root should be added to the project and tracks it for duplicate detection.
     * <p>
     * Rules:
     * <ul>
     *   <li>Disabled sources are always added (they're filtered by {@code getEnabledSourceRoots()})</li>
     *   <li>First enabled source for an identity is added and tracked</li>
     *   <li>Subsequent enabled sources with same identity trigger a WARNING and are NOT added</li>
     * </ul>
     *
     * @param sourceRoot the source root to evaluate
     * @return true if the source should be added to the project, false if it's a duplicate enabled source
     */
    boolean shouldAddSource(SourceRoot sourceRoot) {
        if (!sourceRoot.enabled()) {
            // Disabled sources are always added - they're filtered out by getEnabledSourceRoots()
            LOGGER.trace(
                    "Adding disabled source (will be filtered by getEnabledSourceRoots): lang={}, scope={}, module={}, dir={}",
                    sourceRoot.language(),
                    sourceRoot.scope(),
                    sourceRoot.module().orElse(null),
                    sourceRoot.directory());
            return true;
        }

        // Normalize path for consistent duplicate detection (handles symlinks, relative paths)
        Path normalizedDir = sourceRoot.directory().toAbsolutePath().normalize();
        SourceKey key = new SourceKey(
                sourceRoot.language(), sourceRoot.scope(), sourceRoot.module().orElse(null), normalizedDir);

        if (declaredSources.contains(key)) {
            String message = String.format(
                    "Duplicate enabled source detected: lang=%s, scope=%s, module=%s, directory=%s. "
                            + "First enabled source wins, this duplicate is ignored.",
                    key.language(), key.scope(), key.module() != null ? key.module() : "(none)", key.directory());
            LOGGER.warn(message);
            result.getProblemCollector()
                    .reportProblem(new DefaultModelProblem(
                            message,
                            Severity.WARNING,
                            Version.V41,
                            project.getModel().getDelegate(),
                            -1,
                            -1,
                            null));
            return false; // Don't add duplicate enabled source
        }

        declaredSources.add(key);
        LOGGER.debug(
                "Adding and tracking enabled source: lang={}, scope={}, module={}, dir={}",
                key.language(),
                key.scope(),
                key.module(),
                key.directory());
        return true; // Add first enabled source with this identity
    }

    /**
     * Checks if any enabled sources have been declared for the given language and scope combination.
     *
     * @param language the language to check (e.g., {@link Language#JAVA_FAMILY}, {@link Language#RESOURCES})
     * @param scope the scope to check (e.g., {@link ProjectScope#MAIN}, {@link ProjectScope#TEST})
     * @return true if at least one enabled source exists for this combination
     */
    boolean hasSources(Language language, ProjectScope scope) {
        return declaredSources.stream().anyMatch(key -> language.equals(key.language()) && scope.equals(key.scope()));
    }

    /**
     * Validates that a project does not mix modular and classic (non-modular) sources.
     * <p>
     * A project must be either fully modular (all sources have a module) or fully classic
     * (no sources have a module). Mixing modular and non-modular sources within the same
     * project is not supported because the compiler plugin cannot handle such configurations.
     * <p>
     * This validation checks each (language, scope) combination and reports an ERROR if
     * both modular and non-modular sources are found.
     */
    void validateNoMixedModularAndClassicSources() {
        for (ProjectScope scope : List.of(ProjectScope.MAIN, ProjectScope.TEST)) {
            for (Language language : List.of(Language.JAVA_FAMILY, Language.RESOURCES)) {
                boolean hasModular = declaredSources.stream()
                        .anyMatch(key ->
                                language.equals(key.language()) && scope.equals(key.scope()) && key.module() != null);
                boolean hasClassic = declaredSources.stream()
                        .anyMatch(key ->
                                language.equals(key.language()) && scope.equals(key.scope()) && key.module() == null);

                if (hasModular && hasClassic) {
                    String message = String.format(
                            "Mixed modular and classic sources detected for lang=%s, scope=%s. "
                                    + "A project must be either fully modular (all sources have a module) "
                                    + "or fully classic (no sources have a module). "
                                    + "The compiler plugin cannot handle mixed configurations.",
                            language.id(), scope.id());
                    LOGGER.error(message);
                    result.getProblemCollector()
                            .reportProblem(new DefaultModelProblem(
                                    message,
                                    Severity.ERROR,
                                    Version.V41,
                                    project.getModel().getDelegate(),
                                    -1,
                                    -1,
                                    null));
                }
            }
        }
    }

    /**
     * Handles resource configuration for a given scope (main or test).
     * This method applies the resource priority rules:
     * <ol>
     *   <li>Modular project: use resources from {@code <sources>} if present, otherwise inject defaults</li>
     *   <li>Classic project: use resources from {@code <sources>} if present, otherwise use legacy resources</li>
     * </ol>
     *
     * @param scope the project scope (MAIN or TEST)
     */
    void handleResourceConfiguration(ProjectScope scope) {
        boolean hasResourcesInSources = hasSources(Language.RESOURCES, scope);

        List<Resource> resources = scope == ProjectScope.MAIN
                ? project.getBuild().getDelegate().getResources()
                : project.getBuild().getDelegate().getTestResources();

        String scopeId = scope.id();
        String scopeName = scope == ProjectScope.MAIN ? "Main" : "Test";
        String legacyElement = scope == ProjectScope.MAIN ? "<resources>" : "<testResources>";
        String sourcesConfig = scope == ProjectScope.MAIN
                ? "<source><lang>resources</lang></source>"
                : "<source><lang>resources</lang><scope>test</scope></source>";

        if (modularProject) {
            if (hasResourcesInSources) {
                // Modular project with resources configured via <sources> - already added above
                if (hasExplicitLegacyResources(resources, scopeId)) {
                    LOGGER.warn(
                            "Legacy {} element is ignored because {} resources are configured via {} in <sources>.",
                            legacyElement,
                            scopeId,
                            sourcesConfig);
                } else {
                    LOGGER.debug(
                            "{} resources configured via <sources> element, ignoring legacy {} element.",
                            scopeName,
                            legacyElement);
                }
            } else {
                // Modular project without resources in <sources> - inject module-aware defaults
                if (hasExplicitLegacyResources(resources, scopeId)) {
                    String message = "Legacy " + legacyElement
                            + " element is ignored because modular sources are configured. "
                            + "Use " + sourcesConfig + " in <sources> for custom resource paths.";
                    LOGGER.warn(message);
                    result.getProblemCollector()
                            .reportProblem(new DefaultModelProblem(
                                    message,
                                    Severity.WARNING,
                                    Version.V41,
                                    project.getModel().getDelegate(),
                                    -1,
                                    -1,
                                    null));
                }
                for (String module : modules) {
                    project.addSourceRoot(createModularResourceRoot(module, scope));
                }
                if (!modules.isEmpty()) {
                    LOGGER.debug(
                            "Injected {} module-aware {} resource root(s) for modules: {}.",
                            modules.size(),
                            scopeId,
                            modules);
                }
            }
        } else {
            // Classic (non-modular) project
            if (hasResourcesInSources) {
                // Resources configured via <sources> - already added above
                if (hasExplicitLegacyResources(resources, scopeId)) {
                    LOGGER.warn(
                            "Legacy {} element is ignored because {} resources are configured via {} in <sources>.",
                            legacyElement,
                            scopeId,
                            sourcesConfig);
                } else {
                    LOGGER.debug(
                            "{} resources configured via <sources> element, ignoring legacy {} element.",
                            scopeName,
                            legacyElement);
                }
            } else {
                // Use legacy resources element
                LOGGER.debug(
                        "Using explicit or default {} resources ({} resources configured).", scopeId, resources.size());
                for (Resource resource : resources) {
                    project.addSourceRoot(new DefaultSourceRoot(baseDir, scope, resource));
                }
            }
        }
    }

    /**
     * Creates a DefaultSourceRoot for module-aware resource directories.
     * Generates paths following the pattern: {@code src/<module>/<scope>/resources}
     *
     * @param module module name
     * @param scope project scope (main or test)
     * @return configured DefaultSourceRoot for the module's resources
     */
    private DefaultSourceRoot createModularResourceRoot(String module, ProjectScope scope) {
        Path resourceDir =
                baseDir.resolve("src").resolve(module).resolve(scope.id()).resolve("resources");

        return new DefaultSourceRoot(
                scope,
                Language.RESOURCES,
                module,
                null, // targetVersion
                resourceDir,
                null, // includes
                null, // excludes
                false, // stringFiltering
                Path.of(module), // targetPath - resources go to target/classes/<module>
                true // enabled
                );
    }

    /**
     * Checks if the given resource list contains explicit legacy resources that differ
     * from Super POM defaults. Super POM defaults are: src/{scope}/resources and src/{scope}/resources-filtered
     *
     * @param resources list of resources to check
     * @param scope scope (main or test)
     * @return true if explicit legacy resources are present that would be ignored
     */
    private boolean hasExplicitLegacyResources(List<Resource> resources, String scope) {
        if (resources.isEmpty()) {
            return false; // No resources means no explicit legacy resources to warn about
        }

        // Super POM default paths
        String defaultPath =
                baseDir.resolve("src").resolve(scope).resolve("resources").toString();
        String defaultFilteredPath = baseDir.resolve("src")
                .resolve(scope)
                .resolve("resources-filtered")
                .toString();

        // Check if any resource differs from Super POM defaults
        for (Resource resource : resources) {
            String resourceDir = resource.getDirectory();
            if (resourceDir != null && !resourceDir.equals(defaultPath) && !resourceDir.equals(defaultFilteredPath)) {
                // Found an explicit legacy resource
                return true;
            }
        }

        return false;
    }
}
