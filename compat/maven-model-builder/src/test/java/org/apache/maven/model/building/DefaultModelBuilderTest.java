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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
@Deprecated
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

        assertThrows(ModelBuildingException.class, () -> builder.build(request));
    }

    static class CycleInImportsResolver extends BaseModelResolver {
        @Override
        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
            return switch (dependency.getManagementKey()) {
                case BASE1_ID -> new StringModelSource(BASE1);
                case BASE2_ID -> new StringModelSource(BASE2);
                default -> null;
            };
        }
    }

    /**
     * Verifies that a profile activated by a <em>negated</em> property condition
     * ({@code <name>!foo</name>}, no value) is <em>not</em> filtered out during external model
     * builds (VALIDATION_LEVEL_MINIMAL).  Such profiles are on by default — they fire when the
     * property is absent — and blocking them causes missing dependency versions in the effective
     * model, reproducing the regression reported in
     * <a href="https://github.com/apache/maven/issues/13084">GH-13084</a>.
     */
    @Test
    void negatedPropertyActivatedProfileIsPreservedInExternalModelBuild() throws Exception {
        // A POM whose default profile ("my-default") activates when "skip.defaults" is absent.
        // The profile provides the version for a dependency that has no version outside the profile.
        String pom = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>org.example</groupId>\n"
                + "  <artifactId>mylib</artifactId>\n"
                + "  <version>1.0</version>\n"
                + "  <packaging>jar</packaging>\n"
                + "  <profiles>\n"
                + "    <profile>\n"
                + "      <id>my-default</id>\n"
                + "      <activation>\n"
                + "        <property>\n"
                + "          <name>!skip.defaults</name>\n"
                + "        </property>\n"
                + "      </activation>\n"
                + "      <dependencies>\n"
                + "        <dependency>\n"
                + "          <groupId>org.example</groupId>\n"
                + "          <artifactId>dep-a</artifactId>\n"
                + "          <version>2.0</version>\n"
                + "        </dependency>\n"
                + "      </dependencies>\n"
                + "    </profile>\n"
                + "  </profiles>\n"
                + "</project>\n";

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(pom));
        // External model build: VALIDATION_LEVEL_MINIMAL is used by the artifact descriptor reader
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setModelResolver(new BaseModelResolver());

        ModelBuildingResult result = builder.build(request);
        List<org.apache.maven.model.Dependency> deps =
                result.getEffectiveModel().getDependencies();

        assertTrue(
                deps.stream().anyMatch(d -> "dep-a".equals(d.getArtifactId())),
                "dep-a must be present: negated-property profile must activate in external model builds");
        assertEquals(
                "2.0",
                deps.stream()
                        .filter(d -> "dep-a".equals(d.getArtifactId()))
                        .findFirst()
                        .map(org.apache.maven.model.Dependency::getVersion)
                        .orElse(null),
                "dep-a version must be 2.0 (injected by the negated-property profile)");
    }

    static class BaseModelResolver implements ModelResolver {
        @Override
        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            return switch (groupId + ":" + artifactId + ":" + version) {
                case BASE1_ID2 -> new StringModelSource(BASE1);
                case BASE2_ID2 -> new StringModelSource(BASE2);
                default -> null;
            };
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
