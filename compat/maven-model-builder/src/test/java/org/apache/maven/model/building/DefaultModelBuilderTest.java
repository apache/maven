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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    // -----------------------------------------------------------------------
    // MNG-5146: parent.relativePath GA mismatch severity
    // -----------------------------------------------------------------------

    private static final String REAL_PARENT = "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>mygroup</groupId>\n"
            + "  <artifactId>myparent</artifactId>\n"
            + "  <version>1.0</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "</project>\n";

    private static final String WRONG_PARENT = "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>wrong</groupId>\n"
            + "  <artifactId>wrong</artifactId>\n"
            + "  <version>1.0</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "</project>\n";

    /**
     * MNG-5146: when {@code <relativePath>} is omitted (null), the default {@code ../pom.xml} is
     * probed and, if its GA does not match the declared parent GA, Maven must emit a WARNING (not a
     * FATAL) and fall back to repository resolution.  The build must succeed and the warning must
     * contain the standard mismatch message.
     */
    @Test
    public void testParentGaMismatchDefaultRelativePathProducesWarning(@TempDir Path tempDir)
            throws IOException, ModelBuildingException {
        // Layout: tempDir/pom.xml (wrong GA) and tempDir/child/pom.xml (no <relativePath>)
        Path parentPom = tempDir.resolve("pom.xml");
        Files.writeString(parentPom, WRONG_PARENT);

        Path childDir = Files.createDirectory(tempDir.resolve("child"));
        Path childPom = childDir.resolve("pom.xml");
        // No <relativePath>: Maven defaults to ../pom.xml → finds parentPom (wrong GA)
        String childContent = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <parent>\n"
                + "    <groupId>mygroup</groupId>\n"
                + "    <artifactId>myparent</artifactId>\n"
                + "    <version>1.0</version>\n"
                + "  </parent>\n"
                + "  <artifactId>mychild</artifactId>\n"
                + "</project>\n";
        Files.writeString(childPom, childContent);

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(childPom.toFile());
        // Resolver returns the *correct* parent for external resolution (fallback after mismatch)
        request.setModelResolver(new ParentProvidingResolver("mygroup", "myparent", "1.0", REAL_PARENT));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        ModelBuildingResult result = builder.build(request);

        // The build must succeed; the mismatch diagnostic must be a WARNING, not a FATAL.
        List<ModelProblem> problems = result.getProblems();
        long warningCount = problems.stream()
                .filter(p -> p.getSeverity() == Severity.WARNING
                        && p.getMessage().contains("please verify your project structure"))
                .count();
        assertEquals(1, warningCount, "Expected exactly one WARNING about GA mismatch; got: " + problems);

        // No FATAL problem for this mismatch.
        boolean hasFatalMismatch = problems.stream()
                .anyMatch(p -> p.getSeverity() == Severity.FATAL
                        && p.getMessage().contains("please verify your project structure"));
        assertTrue(!hasFatalMismatch, "Expected no FATAL for default-relativePath mismatch; got: " + problems);
    }

    /**
     * MNG-5146: when {@code <relativePath>} is set explicitly and points to a POM whose GA does
     * not match the declared parent, Maven must emit a FATAL error and throw
     * {@link ModelBuildingException}.
     */
    @Test
    public void testParentGaMismatchExplicitRelativePathProducesFatal(@TempDir Path tempDir) throws IOException {
        // Layout: tempDir/pom.xml (wrong GA) and tempDir/child/pom.xml (explicit relativePath)
        Path parentPom = tempDir.resolve("pom.xml");
        Files.writeString(parentPom, WRONG_PARENT);

        Path childDir = Files.createDirectory(tempDir.resolve("child"));
        Path childPom = childDir.resolve("pom.xml");
        // <relativePath> explicitly points at ../pom.xml → same file, but wrong GA → must be FATAL
        String childContent = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <parent>\n"
                + "    <groupId>mygroup</groupId>\n"
                + "    <artifactId>myparent</artifactId>\n"
                + "    <version>1.0</version>\n"
                + "    <relativePath>../pom.xml</relativePath>\n"
                + "  </parent>\n"
                + "  <artifactId>mychild</artifactId>\n"
                + "</project>\n";
        Files.writeString(childPom, childContent);

        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(childPom.toFile());
        request.setModelResolver(new ParentProvidingResolver("mygroup", "myparent", "1.0", REAL_PARENT));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        ModelBuildingException ex = assertThrows(ModelBuildingException.class, () -> builder.build(request));

        // The exception must contain a FATAL problem for this mismatch.
        boolean hasFatalMismatch = ex.getProblems().stream()
                .anyMatch(p -> p.getSeverity() == Severity.FATAL
                        && p.getMessage().contains("please verify your project structure"));
        assertTrue(
                hasFatalMismatch,
                "Expected a FATAL problem for explicit-relativePath mismatch; got: " + ex.getProblems());
    }

    /**
     * Minimal {@link ModelResolver} that returns a fixed POM for one known parent GAV and
     * null/exception for everything else.
     */
    static class ParentProvidingResolver extends BaseModelResolver {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String pom;

        ParentProvidingResolver(String groupId, String artifactId, String version, String pom) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.pom = pom;
        }

        @Override
        public ModelSource resolveModel(String g, String a, String v) throws UnresolvableModelException {
            if (groupId.equals(g) && artifactId.equals(a) && version.equals(v)) {
                return new StringModelSource(pom);
            }
            return super.resolveModel(g, a, v);
        }

        @Override
        public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }
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
