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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
class BomRelocationTest {
    private final Map<String, Integer> resolutions = new HashMap<>();

    @Test
    void importsRelocatedBom() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        Model model = build(poms, management(bom("old")));
        assertEquals(List.of("library:2"), managedDependencies(model));
    }

    @Test
    void followsPartialRelocationChain() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<groupId>relocated</groupId>")),
                "relocated:old:1", pom("relocated", "old", "1", relocation("<artifactId>new</artifactId>")),
                "relocated:new:1", pom("relocated", "new", "1", relocation("<version>2</version>")),
                "relocated:new:2", pom("relocated", "new", "2", management(dependency("library", "2"))));
        assertEquals(List.of("library:2"), managedDependencies(build(poms, management(bom("old")))));
    }

    @Test
    void ignoresManagementFromRelocationSourceAndPreservesPrecedence() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                        pom(
                                "test",
                                "old",
                                "1",
                                relocation("<artifactId>new</artifactId>") + management(dependency("obsolete", "1"))),
                "test:new:1",
                        pom("test", "new", "1", management(dependency("library", "2") + dependency("other", "2"))),
                "test:ordinary:1", pom("test", "ordinary", "1", management(dependency("other", "3"))));
        String content = management(dependency("library", "4") + bom("old") + bom("ordinary"));
        assertEquals(List.of("library:4", "other:2"), managedDependencies(build(poms, content)));
    }

    @Test
    void preservesManagementForUnchangedRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                pom(
                        "test",
                        "old",
                        "1",
                        relocation("<artifactId>old</artifactId>") + management(dependency("library", "2"))));
        assertEquals(List.of("library:2"), managedDependencies(build(poms, management(bom("old")))));
    }

    @Test
    void preservesManagementForMessageOnlyRelocation() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1",
                pom(
                        "test",
                        "old",
                        "1",
                        relocation("<message>Use the current release</message>")
                                + management(dependency("library", "2"))));
        assertEquals(List.of("library:2"), managedDependencies(build(poms, management(bom("old")))));
    }

    @Test
    void detectsRelocationCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                        "test:new:1", pom("test", "new", "1", relocation("<artifactId>old</artifactId>"))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void detectsRelocationThenImportCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                        "test:new:1", pom("test", "new", "1", management(bom("old")))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void detectsImportThenRelocationCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", management(bom("new"))),
                        "test:new:1", pom("test", "new", "1", relocation("<artifactId>old</artifactId>"))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void detectsImportCycle() {
        assertCycle(
                Map.of(
                        "test:old:1", pom("test", "old", "1", management(bom("new"))),
                        "test:new:1", pom("test", "new", "1", management(bom("old")))),
                "test:consumer:1 -> test:old:1 -> test:new:1 -> test:old:1");
    }

    @Test
    void reportsUnresolvableRelocationTarget() {
        Map<String, String> poms =
                Map.of("test:old:1", pom("test", "old", "1", relocation("<artifactId>missing</artifactId>")));
        ModelBuildingException exception =
                assertThrows(ModelBuildingException.class, () -> build(poms, management(bom("old"))));
        assertTrue(exception.getMessage().contains("Non-resolvable import POM"), exception.getMessage());
        assertTrue(exception.getMessage().contains("test:missing"), exception.getMessage());
    }

    @Test
    void importsRelocatedWorkspaceBom() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        assertEquals(List.of("library:2"), managedDependencies(build(poms, management(bom("old")), true)));
    }

    @Test
    void rejectsInvalidRelocationCoordinates() {
        for (String component : List.of("groupId", "artifactId", "version")) {
            for (String value : List.of("..", "bad/name", "bad\\name", "bad:name", "bad&#9;name")) {
                Map<String, String> poms = Map.of(
                        "test:old:1",
                        pom("test", "old", "1", relocation("<" + component + ">" + value + "</" + component + ">")));
                ModelBuildingException exception =
                        assertThrows(ModelBuildingException.class, () -> build(poms, management(bom("old"))));
                assertTrue(exception.getMessage().contains("Invalid relocation " + component), exception.getMessage());
            }
        }
    }

    @Test
    void repeatedImportsReuseRelocatedManagement() throws Exception {
        Map<String, String> poms = Map.of(
                "test:old:1", pom("test", "old", "1", relocation("<artifactId>new</artifactId>")),
                "test:alias:1", pom("test", "alias", "1", relocation("<artifactId>new</artifactId>")),
                "test:new:1", pom("test", "new", "1", management(dependency("library", "2"))));
        assertEquals(
                List.of("library:2"),
                managedDependencies(build(poms, management(bom("old") + bom("alias") + bom("new")))));
        assertEquals(1, resolutions.get("test:new:1"));
    }

    private void assertCycle(Map<String, String> poms, String cycle) {
        ModelBuildingException exception =
                assertThrows(ModelBuildingException.class, () -> build(poms, management(bom("old"))));
        assertTrue(exception.getMessage().contains("The import POMs form a cycle: " + cycle), exception.getMessage());
    }

    private Model build(Map<String, String> poms, String content) throws Exception {
        return build(poms, content, false);
    }

    private Model build(Map<String, String> poms, String content, boolean workspace) throws Exception {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom("test", "consumer", "1", content)));
        request.setModelResolver(new DefaultModelBuilderTest.BaseModelResolver() {
            @Override
            public ModelSource resolveModel(String groupId, String artifactId, String version)
                    throws UnresolvableModelException {
                String id = groupId + ":" + artifactId + ":" + version;
                resolutions.merge(id, 1, Integer::sum);
                String source = poms.get(id);
                if (source == null) {
                    throw new UnresolvableModelException("Missing " + id, groupId, artifactId, version);
                }
                return new StringModelSource(source, id);
            }
        });
        Map<String, Object> cache = new HashMap<>();
        request.setModelCache(new ModelCache() {
            @Override
            public void put(String groupId, String artifactId, String version, String tag, Object data) {
                cache.put(groupId + ":" + artifactId + ":" + version + ":" + tag, data);
            }

            @Override
            public Object get(String groupId, String artifactId, String version, String tag) {
                return cache.get(groupId + ":" + artifactId + ":" + version + ":" + tag);
            }
        });
        if (workspace) {
            request.setModelResolver(null);
            request.setWorkspaceModelResolver(new WorkspaceModelResolver() {
                @Override
                public Model resolveRawModel(String groupId, String artifactId, String version) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Model resolveEffectiveModel(String groupId, String artifactId, String version)
                        throws UnresolvableModelException {
                    try {
                        return new MavenXpp3Reader()
                                .read(new StringReader(poms.get(groupId + ":" + artifactId + ":" + version)));
                    } catch (Exception e) {
                        throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
                    }
                }
            });
        }
        return new DefaultModelBuilderFactory().newInstance().build(request).getEffectiveModel();
    }

    private List<String> managedDependencies(Model model) {
        return model.getDependencyManagement().getDependencies().stream()
                .map(d -> d.getArtifactId() + ":" + d.getVersion())
                .toList();
    }

    private static String pom(String groupId, String artifactId, String version, String content) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>" + groupId
                + "</groupId><artifactId>" + artifactId + "</artifactId><version>" + version
                + "</version><packaging>pom</packaging>" + content + "</project>";
    }

    private static String relocation(String coordinates) {
        return "<distributionManagement><relocation>" + coordinates + "</relocation></distributionManagement>";
    }

    private static String management(String dependencies) {
        return "<dependencyManagement><dependencies>" + dependencies + "</dependencies></dependencyManagement>";
    }

    private static String dependency(String artifactId, String version) {
        return "<dependency><groupId>test</groupId><artifactId>" + artifactId + "</artifactId><version>" + version
                + "</version></dependency>";
    }

    private static String bom(String artifactId) {
        return "<dependency><groupId>test</groupId><artifactId>" + artifactId
                + "</artifactId><version>1</version><type>pom</type><scope>import</scope></dependency>";
    }
}
