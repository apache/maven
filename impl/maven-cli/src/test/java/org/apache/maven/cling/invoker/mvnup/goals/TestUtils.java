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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utility class for creating test fixtures and reducing code duplication in tests.
 */
public final class TestUtils {

    private TestUtils() {
        // Utility class
    }

    /**
     * Creates a mock UpgradeContext with default settings.
     *
     * @return a mock UpgradeContext
     */
    public static UpgradeContext createMockContext() {
        return createMockContext(Paths.get("/project"));
    }

    /**
     * Creates a mock UpgradeContext with the specified working directory.
     *
     * @param workingDirectory the working directory to use
     * @return a mock UpgradeContext
     */
    public static UpgradeContext createMockContext(Path workingDirectory) {
        return createMockContext(workingDirectory, createDefaultOptions());
    }

    /**
     * Creates a mock UpgradeContext with the specified options.
     *
     * @param options the upgrade options to use
     * @return a mock UpgradeContext
     */
    public static UpgradeContext createMockContext(UpgradeOptions options) {
        return createMockContext(Paths.get("/project"), options);
    }

    /**
     * Creates a mock UpgradeContext with the specified working directory and options.
     *
     * @param workingDirectory the working directory to use
     * @param options the upgrade options to use
     * @return a mock UpgradeContext
     */
    public static UpgradeContext createMockContext(Path workingDirectory, UpgradeOptions options) {
        InvokerRequest request = mock(InvokerRequest.class);

        // Mock all required properties for LookupContext constructor
        when(request.cwd()).thenReturn(workingDirectory);
        when(request.installationDirectory()).thenReturn(Paths.get("/maven"));
        when(request.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        when(request.topDirectory()).thenReturn(workingDirectory);
        when(request.rootDirectory()).thenReturn(Optional.empty());
        when(request.userProperties()).thenReturn(Map.of());
        when(request.systemProperties()).thenReturn(Map.of());
        when(request.options()).thenReturn(Optional.ofNullable(options));

        // Mock parserRequest and logger
        ParserRequest parserRequest = mock(ParserRequest.class);
        Logger logger = mock(Logger.class);
        when(request.parserRequest()).thenReturn(parserRequest);
        when(parserRequest.logger()).thenReturn(logger);

        return new UpgradeContext(request, options);
    }

    /**
     * Creates default upgrade options with all optional values empty.
     *
     * @return default upgrade options
     */
    public static UpgradeOptions createDefaultOptions() {
        UpgradeOptions options = mock(UpgradeOptions.class);
        when(options.all()).thenReturn(Optional.empty());
        when(options.infer()).thenReturn(Optional.empty());
        when(options.model()).thenReturn(Optional.empty());
        when(options.plugins()).thenReturn(Optional.empty());
        when(options.modelVersion()).thenReturn(Optional.empty());
        return options;
    }

    /**
     * Creates upgrade options with specific values.
     *
     * @param all the --all option value (null for absent)
     * @param infer the --infer option value (null for absent)
     * @param model the --model option value (null for absent)
     * @param plugins the --plugins option value (null for absent)
     * @param modelVersion the --model-version option value (null for absent)
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptions(
            Boolean all, Boolean infer, Boolean model, Boolean plugins, String modelVersion) {
        UpgradeOptions options = mock(UpgradeOptions.class);
        when(options.all()).thenReturn(Optional.ofNullable(all));
        when(options.infer()).thenReturn(Optional.ofNullable(infer));
        when(options.model()).thenReturn(Optional.ofNullable(model));
        when(options.plugins()).thenReturn(Optional.ofNullable(plugins));
        when(options.modelVersion()).thenReturn(Optional.ofNullable(modelVersion));
        return options;
    }

    /**
     * Creates upgrade options with only the --all flag set.
     *
     * @param all the --all option value
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptionsWithAll(boolean all) {
        return createOptions(all, null, null, null, null);
    }

    /**
     * Creates upgrade options with only the --model-version option set.
     *
     * @param modelVersion the --model-version option value
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptionsWithModelVersion(String modelVersion) {
        return createOptions(null, null, null, null, modelVersion);
    }

    /**
     * Creates upgrade options with only the --plugins option set.
     *
     * @param plugins the --plugins option value
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptionsWithPlugins(boolean plugins) {
        return createOptions(null, null, null, plugins, null);
    }

    /**
     * Creates upgrade options with only the --fix-model option set.
     *
     * @param fixModel the --fix-model option value
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptionsWithFixModel(boolean fixModel) {
        return createOptions(null, null, fixModel, null, null);
    }

    /**
     * Creates upgrade options with only the --infer option set.
     *
     * @param infer the --infer option value
     * @return configured upgrade options
     */
    public static UpgradeOptions createOptionsWithInfer(boolean infer) {
        return createOptions(null, infer, null, null, null);
    }

    /**
     * Creates a simple POM XML string for testing.
     *
     * @param groupId the group ID
     * @param artifactId the artifact ID
     * @param version the version
     * @return POM XML string
     */
    public static String createSimplePom(String groupId, String artifactId, String version) {
        return String.format(
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </project>
            """,
                groupId, artifactId, version);
    }

    /**
     * Creates a POM XML string with parent for testing.
     *
     * @param parentGroupId the parent group ID
     * @param parentArtifactId the parent artifact ID
     * @param parentVersion the parent version
     * @param artifactId the artifact ID
     * @return POM XML string with parent
     */
    public static String createPomWithParent(
            String parentGroupId, String parentArtifactId, String parentVersion, String artifactId) {
        return String.format(
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                </parent>
                <artifactId>%s</artifactId>
            </project>
            """,
                parentGroupId, parentArtifactId, parentVersion, artifactId);
    }
}
