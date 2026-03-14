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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.Source;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link DefaultProjectBuilder} extractProjectId method.
 */
@SuppressWarnings("deprecation")
class DefaultProjectBuilderTest {

    /**
     * Test the extractProjectId method to ensure it properly falls back to rawModel or fileModel
     * when effectiveModel is null, addressing issue #11292.
     */
    @Test
    void testExtractProjectIdFallback() throws Exception {
        // Use reflection to access the private extractProjectId method
        Method extractProjectIdMethod =
                DefaultProjectBuilder.class.getDeclaredMethod("extractProjectId", ModelBuilderResult.class);
        extractProjectIdMethod.setAccessible(true);

        // Create a mock ModelBuilderResult with null effectiveModel but available rawModel
        ModelBuilderResult mockResult = new MockModelBuilderResult(
                null, // effectiveModel is null
                createMockModel("com.example", "test-project", "1.0.0"), // rawModel is available
                null // fileModel is null
                );

        String projectId = (String) extractProjectIdMethod.invoke(null, mockResult);

        assertNotNull(projectId, "Project ID should not be null");
        assertEquals(
                "com.example:test-project:jar:1.0.0",
                projectId,
                "Should extract project ID from rawModel when effectiveModel is null");
    }

    /**
     * Test extractProjectId with fileModel fallback when both effectiveModel and rawModel are null.
     */
    @Test
    void testExtractProjectIdFileModelFallback() throws Exception {
        Method extractProjectIdMethod =
                DefaultProjectBuilder.class.getDeclaredMethod("extractProjectId", ModelBuilderResult.class);
        extractProjectIdMethod.setAccessible(true);

        ModelBuilderResult mockResult = new MockModelBuilderResult(
                null, // effectiveModel is null
                null, // rawModel is null
                createMockModel("com.example", "test-project", "1.0.0") // fileModel is available
                );

        String projectId = (String) extractProjectIdMethod.invoke(null, mockResult);

        assertNotNull(projectId, "Project ID should not be null");
        assertEquals(
                "com.example:test-project:jar:1.0.0",
                projectId,
                "Should extract project ID from fileModel when effectiveModel and rawModel are null");
    }

    /**
     * Test extractProjectId returns empty string when all models are null.
     */
    @Test
    void testExtractProjectIdAllModelsNull() throws Exception {
        Method extractProjectIdMethod =
                DefaultProjectBuilder.class.getDeclaredMethod("extractProjectId", ModelBuilderResult.class);
        extractProjectIdMethod.setAccessible(true);

        ModelBuilderResult mockResult = new MockModelBuilderResult(null, null, null);

        String projectId = (String) extractProjectIdMethod.invoke(null, mockResult);

        assertNotNull(projectId, "Project ID should not be null");
        assertEquals("", projectId, "Should return empty string when all models are null");
    }

    private Model createMockModel(String groupId, String artifactId, String version) {
        return Model.newBuilder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .packaging("jar")
                .build();
    }

    /**
     * Mock implementation of ModelBuilderResult for testing.
     */
    private static class MockModelBuilderResult implements ModelBuilderResult {
        private final Model effectiveModel;
        private final Model rawModel;
        private final Model fileModel;

        MockModelBuilderResult(Model effectiveModel, Model rawModel, Model fileModel) {
            this.effectiveModel = effectiveModel;
            this.rawModel = rawModel;
            this.fileModel = fileModel;
        }

        @Override
        public Model getEffectiveModel() {
            return effectiveModel;
        }

        @Override
        public Model getRawModel() {
            return rawModel;
        }

        @Override
        public Model getFileModel() {
            return fileModel;
        }

        @Override
        public ModelBuilderRequest getRequest() {
            return null;
        }

        // Other required methods with minimal implementations
        @Override
        public ModelSource getSource() {
            return new ModelSource() {
                @Override
                public Path getPath() {
                    return Paths.get("test-pom.xml");
                }

                @Override
                public String getLocation() {
                    return "test-pom.xml";
                }

                @Override
                public InputStream openStream() throws IOException {
                    return null;
                }

                @Override
                public Source resolve(String relative) {
                    return null;
                }

                @Override
                public ModelSource resolve(ModelSource.ModelLocator modelLocator, String relative) {
                    return null;
                }
            };
        }

        @Override
        public Model getParentModel() {
            return null;
        }

        @Override
        public List<Profile> getActivePomProfiles() {
            return List.of();
        }

        @Override
        public List<Profile> getActivePomProfiles(String modelId) {
            return List.of();
        }

        @Override
        public java.util.Map<String, List<Profile>> getActivePomProfilesByModel() {
            return java.util.Map.of();
        }

        @Override
        public List<Profile> getActiveExternalProfiles() {
            return List.of();
        }

        @Override
        public ProblemCollector<ModelProblem> getProblemCollector() {
            return null;
        }

        @Override
        public List<? extends ModelBuilderResult> getChildren() {
            return List.of();
        }
    }
}
