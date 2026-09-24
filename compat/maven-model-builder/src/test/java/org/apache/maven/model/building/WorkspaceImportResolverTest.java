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
package org.apache.maven.model.building;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
class WorkspaceImportResolverTest {
    private static final String IMPORTED = "<project><modelVersion>4.0.0</modelVersion>"
            + "<groupId>test</groupId><artifactId>old</artifactId><version>1</version>"
            + "<dependencyManagement><dependencies><dependency>"
            + "<groupId>test</groupId><artifactId>library</artifactId><version>2</version>"
            + "</dependency></dependencies></dependencyManagement></project>";

    @Test
    void reportsWorkspaceMissWithoutFallbackResolver() {
        ModelBuildingException exception =
                assertThrows(ModelBuildingException.class, () -> build(workspaceReturning(null), null));
        ModelProblem problem = exception.getProblems().stream()
                .filter(candidate -> candidate.getMessage().contains("no ModelResolver provided"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(exception.getMessage()));
        assertEquals(ModelProblem.Severity.ERROR, problem.getSeverity());
        assertTrue(problem.getMessage().contains("test:old:1"));
        assertEquals("test:consumer:1", problem.getModelId());
        assertTrue(problem.getLineNumber() > 0);
        assertTrue(problem.getColumnNumber() > 0);
    }

    @Test
    void usesWorkspaceModelWithoutFallbackResolver() throws Exception {
        ModelBuildingResult result = build(workspaceReturning(readImportedModel()), null);
        assertEquals(
                "2",
                result.getEffectiveModel()
                        .getDependencyManagement()
                        .getDependencies()
                        .get(0)
                        .getVersion());
    }

    @Test
    void retainsCompatBehaviorForRelocatedBom() throws Exception {
        String relocated = IMPORTED.replace(
                "<dependencyManagement>",
                "<distributionManagement><relocation><artifactId>new</artifactId></relocation>"
                        + "</distributionManagement><dependencyManagement>");
        Model model = new MavenXpp3Reader().read(new StringReader(relocated));
        ModelBuildingResult result = build(workspaceReturning(model), null);
        assertEquals(
                "2",
                result.getEffectiveModel()
                        .getDependencyManagement()
                        .getDependencies()
                        .get(0)
                        .getVersion());
    }

    @Test
    void fallsBackAfterWorkspaceMiss() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        ModelBuildingResult result = build(workspaceReturning(null), new DefaultModelBuilderTest.BaseModelResolver() {
            @Override
            public ModelSource resolveModel(String groupId, String artifactId, String version) {
                resolutions.incrementAndGet();
                return new StringModelSource(IMPORTED);
            }
        });
        assertEquals(1, resolutions.get());
        assertEquals(
                "2",
                result.getEffectiveModel()
                        .getDependencyManagement()
                        .getDependencies()
                        .get(0)
                        .getVersion());
    }

    @Test
    void workspaceFailureDoesNotUseFallbackResolver() {
        AtomicInteger resolutions = new AtomicInteger();
        WorkspaceModelResolver workspace = new WorkspaceModelResolver() {
            @Override
            public Model resolveRawModel(String groupId, String artifactId, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Model resolveEffectiveModel(String groupId, String artifactId, String version)
                    throws UnresolvableModelException {
                throw new UnresolvableModelException("Workspace failure", groupId, artifactId, version);
            }
        };
        ModelBuildingException exception = assertThrows(
                ModelBuildingException.class,
                () -> build(workspace, new DefaultModelBuilderTest.BaseModelResolver() {
                    @Override
                    public ModelSource resolveModel(String groupId, String artifactId, String version) {
                        resolutions.incrementAndGet();
                        return new StringModelSource(IMPORTED);
                    }
                }));
        assertEquals(0, resolutions.get());
        assertTrue(exception.getProblems().stream()
                .anyMatch(problem -> problem.getSeverity() == ModelProblem.Severity.FATAL));
    }

    private static WorkspaceModelResolver workspaceReturning(Model imported) {
        return new WorkspaceModelResolver() {
            @Override
            public Model resolveRawModel(String groupId, String artifactId, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Model resolveEffectiveModel(String groupId, String artifactId, String version) {
                return imported;
            }
        };
    }

    private static Model readImportedModel() throws Exception {
        return new MavenXpp3Reader().read(new StringReader(IMPORTED));
    }

    private static ModelBuildingResult build(
            WorkspaceModelResolver workspace, DefaultModelBuilderTest.BaseModelResolver fallback)
            throws ModelBuildingException {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource("<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>test</groupId><artifactId>consumer</artifactId><version>1</version>"
                + "<dependencyManagement><dependencies><dependency>"
                + "<groupId>test</groupId><artifactId>old</artifactId><version>1</version>"
                + "<type>pom</type><scope>import</scope>"
                + "</dependency></dependencies></dependencyManagement></project>"));
        request.setLocationTracking(true);
        request.setWorkspaceModelResolver(workspace);
        request.setModelResolver(fallback);
        return new DefaultModelBuilderFactory().newInstance().build(request);
    }
}
