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
import java.util.List;
import java.util.Set;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.impl.DefaultSourceRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles resource configuration for Maven projects.
 * Groups parameters shared between main and test resource handling.
 */
class ResourceHandlingContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHandlingContext.class);

    private final MavenProject project;
    private final Path baseDir;
    private final Set<String> modules;
    private final boolean modularProject;
    private final ModelBuilderResult result;

    ResourceHandlingContext(
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
     * @param hasResourcesInSources whether resources are configured via {@code <sources>}
     */
    void handleResourceConfiguration(ProjectScope scope, boolean hasResourcesInSources) {
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
                            .reportProblem(new org.apache.maven.impl.model.DefaultModelProblem(
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
