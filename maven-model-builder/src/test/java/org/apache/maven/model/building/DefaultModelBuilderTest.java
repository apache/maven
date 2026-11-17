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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Guillaume Nodet
 */
public class DefaultModelBuilderTest {

    private static final String BASE1_ID = "thegroup:base1:pom";
    private static final String BASE1_ID2 = "thegroup:base1:1";

    private static final String BASE1 = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>thegroup</groupId>\n"
            + "  <artifactId>base1</artifactId>\n"
            + "  <version>1</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <dependencyManagement>\n"
            + "    <dependencies>\n"
            + "      <dependency>\n"
            + "        <groupId>thegroup</groupId>\n"
            + "        <artifactId>base2</artifactId>\n"
            + "        <version>1</version>\n"
            + "        <type>pom</type>\n"
            + "        <scope>import</scope>\n"
            + "      </dependency>\n"
            + "    </dependencies>\n"
            + "  </dependencyManagement>\n"
            + "</project>\n";

    private static final String BASE2_ID = "thegroup:base2:pom";
    private static final String BASE2_ID2 = "thegroup:base2:1";

    private static final String BASE2 = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>thegroup</groupId>\n"
            + "  <artifactId>base2</artifactId>\n"
            + "  <version>1</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <dependencyManagement>\n"
            + "    <dependencies>\n"
            + "      <dependency>\n"
            + "        <groupId>thegroup</groupId>\n"
            + "        <artifactId>base1</artifactId>\n"
            + "        <version>1</version>\n"
            + "        <type>pom</type>\n"
            + "        <scope>import</scope>\n"
            + "      </dependency>\n"
            + "    </dependencies>\n"
            + "  </dependencyManagement>\n"
            + "</project>\n";

    @Test
    public void testCycleInImports() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(BASE1));
        request.setModelResolver(new CycleInImportsResolver());

        assertThrows(ModelBuildingException.class, () -> {
            builder.build(request);
        });
    }

    /**
     * MNG-6864: a well-formed POM that merely has an unrecognized/misplaced element is invalid,
     * not malformed - the "Malformed POM" message is reserved for documents that are not
     * well-formed XML at all.
     */
    @Test
    public void testInvalidPomIsNotReportedAsMalformed() throws Exception {
        String pom = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>invalid</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <dependencies>\n"
                + "    <project>\n"
                + "    </project>\n"
                + "  </dependencies>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom));

        ModelBuildingResult result = builder.build(request);

        assertTrue(
                result.getProblems().stream().anyMatch(p -> p.getMessage().startsWith("Invalid POM")),
                () -> "Expected an \"Invalid POM\" problem, got: " + result.getProblems());
        assertTrue(
                result.getProblems().stream().noneMatch(p -> p.getMessage().startsWith("Malformed POM")),
                () -> "Did not expect a \"Malformed POM\" problem, got: " + result.getProblems());
    }

    /**
     * Contrast case for {@link #testInvalidPomIsNotReportedAsMalformed()}: a document that is not
     * well-formed XML at all must still fail outright, unaffected by MNG-6864.
     */
    @Test
    public void testNotWellFormedPomFailsBuild() {
        String pom = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>malformed</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <name>mismatched</foo>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom));

        ModelBuildingException e = assertThrows(ModelBuildingException.class, () -> builder.build(request));
        assertTrue(
                e.getProblems().stream().anyMatch(p -> p.getMessage().startsWith("Non-parseable POM")),
                () -> "Expected a \"Non-parseable POM\" problem, got: " + e.getProblems());
    }

    /**
     * #13985: an import cached by a build without location tracking must not be reused by a build with it.
     */
    @Test
    public void testImportedDependencyManagementKeepsLocations() throws Exception {
        String bom = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>bom</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <dependencyManagement>\n"
                + "    <dependencies>\n"
                + "      <dependency>\n"
                + "        <groupId>thegroup</groupId>\n"
                + "        <artifactId>managed</artifactId>\n"
                + "        <version>1</version>\n"
                + "      </dependency>\n"
                + "    </dependencies>\n"
                + "  </dependencyManagement>\n"
                + "</project>\n";
        String pom = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>thegroup</groupId>\n"
                + "  <artifactId>consumer</artifactId>\n"
                + "  <version>1</version>\n"
                + "  <dependencyManagement>\n"
                + "    <dependencies>\n"
                + "      <dependency>\n"
                + "        <groupId>thegroup</groupId>\n"
                + "        <artifactId>bom</artifactId>\n"
                + "        <version>1</version>\n"
                + "        <type>pom</type>\n"
                + "        <scope>import</scope>\n"
                + "      </dependency>\n"
                + "    </dependencies>\n"
                + "  </dependencyManagement>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        ModelCache cache = new MapModelCache();
        ModelResolver resolver = new BaseModelResolver() {
            @Override
            public ModelSource resolveModel(String groupId, String artifactId, String version) {
                return new StringModelSource(bom);
            }
        };

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom));
        request.setModelResolver(resolver);
        request.setModelCache(cache);
        request.setLocationTracking(false);
        builder.build(request);

        request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom));
        request.setModelResolver(resolver);
        request.setModelCache(cache);
        request.setLocationTracking(true);
        ModelBuildingResult result = builder.build(request);

        Dependency managed = result.getEffectiveModel()
                .getDependencyManagement()
                .getDependencies()
                .get(0);
        assertNotNull(managed.getLocation("version"));
    }

    static class MapModelCache implements ModelCache {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public void put(String groupId, String artifactId, String version, String tag, Object data) {
            map.put(groupId + ':' + artifactId + ':' + version + ':' + tag, data);
        }

        @Override
        public Object get(String groupId, String artifactId, String version, String tag) {
            return map.get(groupId + ':' + artifactId + ':' + version + ':' + tag);
        }
    }

    static class CycleInImportsResolver extends BaseModelResolver {
        @Override
        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
            switch (dependency.getManagementKey()) {
                case BASE1_ID:
                    return new StringModelSource(BASE1);
                case BASE2_ID:
                    return new StringModelSource(BASE2);
                default:
                    return null;
            }
        }
    }

    static class BaseModelResolver implements ModelResolver {
        @Override
        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            switch (groupId + ":" + artifactId + ":" + version) {
                case BASE1_ID2:
                    return new StringModelSource(BASE1);
                case BASE2_ID2:
                    return new StringModelSource(BASE2);
                default:
                    return null;
            }
        }

        @Override
        public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return null;
        }

        @Override
        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
            return null;
        }

        @Override
        public void addRepository(Repository repository) throws InvalidRepositoryException {}

        @Override
        public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {}

        @Override
        public ModelResolver newCopy() {
            return this;
        }
    }
}
