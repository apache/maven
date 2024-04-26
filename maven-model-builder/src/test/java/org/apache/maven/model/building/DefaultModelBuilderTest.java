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

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Repository;
import org.apache.maven.model.Model;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
class DefaultModelBuilderTest {

    private static final String BASE1_ID = "thegroup:base1:pom";

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
    void testCycleInImports() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(BASE1));
        request.setModelResolver(new CycleInImportsResolver());

        assertThrows(ModelBuildingException.class, () -> builder.build(request));
    }

    static class CycleInImportsResolver extends BaseModelResolver {
        @Override
        public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                throws UnresolvableModelException {
            switch (dependency.getManagementKey()) {
                case BASE1_ID:
                    return new StringModelSource(BASE1);
                case BASE2_ID:
                    return new StringModelSource(BASE2);
            }
            return null;
        }
    }

    static class BaseModelResolver implements ModelResolver {
        @Override
        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            return null;
        }

        @Override
        public ModelSource resolveModel(Parent parent, AtomicReference<Parent> modified)
                throws UnresolvableModelException {
            return null;
        }

        @Override
        public ModelSource resolveModel(Dependency dependency, AtomicReference<Dependency> modified)
                throws UnresolvableModelException {
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

        @Override
        public ModelSource resolveModel(org.apache.maven.model.Parent parent) throws UnresolvableModelException {
            return null;
        }

        @Override
        public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                throws UnresolvableModelException {
            return null;
        }

        @Override
        public void addRepository(org.apache.maven.model.Repository repository) throws InvalidRepositoryException {}

        @Override
        public void addRepository(org.apache.maven.model.Repository repository, boolean replace)
                throws InvalidRepositoryException {}
    }

    @Test
    void testBuildRawModel() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        Result<? extends Model> res = builder.buildRawModel(
                new File(getClass().getResource("/poms/factory/simple.xml").getFile()),
                ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL,
                false);
        assertNotNull(res.get());
    }

    /**
     * This test has
     *   - a dependency directly managed to 0.2
     *   - then a BOM import which manages that same dep to 0.1
     * This <i>currently</i> results in 0.2 and a no warning
     */
    @Test
    void testManagedDependencyBeforeImport() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new FileModelSource(new File(
                getClass().getResource("/poms/depmgmt/root-dep-first.xml").getFile())));
        request.setModelResolver(new BaseModelResolver() {
            public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                    throws UnresolvableModelException {
                switch (dependency.getManagementKey()) {
                    case "test:import:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/import.xml")
                                .getFile()));
                    default:
                        throw new UnresolvableModelException(
                                "Cannot resolve",
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion());
                }
            }
        });

        ModelBuildingResult result = builder.build(request);
        Dependency dep = result.getEffectiveModel().getDelegate().getDependencyManagement().getDependencies().stream()
                .filter(d -> "test:mydep:jar".equals(d.getManagementKey()))
                .findFirst()
                .get();
        assertEquals("0.2", dep.getVersion());
        assertEquals(0, result.getProblems().size());
    }

    /**
     * This test has
     *   - a BOM import which manages the dep to 0.1
     *   - then a directly managed dependency to 0.2
     * This <i>currently</i> results in 0.2 and no warning
     */
    @Test
    void testManagedDependencyAfterImport() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new FileModelSource(new File(
                getClass().getResource("/poms/depmgmt/root-dep-last.xml").getFile())));
        request.setModelResolver(new BaseModelResolver() {
            public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                    throws UnresolvableModelException {
                switch (dependency.getManagementKey()) {
                    case "test:import:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/import.xml")
                                .getFile()));
                    default:
                        throw new UnresolvableModelException(
                                "Cannot resolve",
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion());
                }
            }
        });

        ModelBuildingResult result = builder.build(request);
        Dependency dep = result.getEffectiveModel().getDelegate().getDependencyManagement().getDependencies().stream()
                .filter(d -> "test:mydep:jar".equals(d.getManagementKey()))
                .findFirst()
                .get();
        assertEquals("0.2", dep.getVersion());
        assertEquals(0, result.getProblems().size());
    }

    /**
     * This test has
     *   - a BOM import which manages dep to 0.3
     *   - another BOM import which manages the dep to 0.1
     * This <i>currently</i> results in 0.3 (first wins) and a warning
     */
    @Test
    void testManagedDependencyTwoImports() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new FileModelSource(new File(
                getClass().getResource("/poms/depmgmt/root-two-imports.xml").getFile())));
        request.setModelResolver(new BaseModelResolver() {
            public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                    throws UnresolvableModelException {
                switch (dependency.getManagementKey()) {
                    case "test:import:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/import.xml")
                                .getFile()));
                    case "test:other:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/other-import.xml")
                                .getFile()));
                    default:
                        throw new UnresolvableModelException(
                                "Cannot resolve",
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion());
                }
            }
        });

        ModelBuildingResult result = builder.build(request);
        Dependency dep = result.getEffectiveModel().getDelegate().getDependencyManagement().getDependencies().stream()
                .filter(d -> "test:mydep:jar".equals(d.getManagementKey()))
                .findFirst()
                .get();
        assertEquals("0.3", dep.getVersion());
        assertEquals(1, result.getProblems().size());
        ModelProblem problem = result.getProblems().get(0);
        assertTrue(problem.toString().contains("Ignored POM import"));
    }

    /**
     * This test has
     *   - a BOM import which itself imports the junit BOM which manages the dep to 0.1
     *   - a BOM import to the junit BOM which manages the dep to 0.2
     * This <i>currently</i> results in 0.1 (first wins, unexpected as this is not the closest) and a warning
     */
    @Test
    void testManagedDependencyDistance() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setLocationTracking(true);
        request.setModelSource(new FileModelSource(new File(
                getClass().getResource("/poms/depmgmt/root-distance.xml").getFile())));
        request.setModelResolver(new BaseModelResolver() {
            public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                    throws UnresolvableModelException {
                switch (dependency.getManagementKey()) {
                    case "org.junit:bom:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/junit-" + dependency.getVersion() + ".xml")
                                .getFile()));
                    case "test:other:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/distant-import.xml")
                                .getFile()));
                    default:
                        throw new UnresolvableModelException(
                                "Cannot resolve",
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion());
                }
            }
        });

        ModelBuildingResult result = builder.build(request);
        Dependency dep = result.getEffectiveModel().getDelegate().getDependencyManagement().getDependencies().stream()
                .filter(d -> "org.junit:junit:jar".equals(d.getManagementKey()))
                .findFirst()
                .get();
        assertEquals("0.1", dep.getVersion());
        assertEquals(1, result.getProblems().size());
        ModelProblem problem = result.getProblems().get(0);
        assertTrue(problem.toString().contains("Ignored POM import"));
    }

    /**
     * This test has
     *   - a BOM import which itself imports the junit BOM which manages the dep to 0.1
     *   - a BOM import to the junit BOM which manages the dep to 0.2
     *   - a direct managed dependency to the dep at 0.3 (as suggested by the warning)
     * This results in 0.3 (explicit managed wins) and no warning
     */
    @Test
    void testManagedDependencyDistanceWithExplicit() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setLocationTracking(true);
        request.setModelSource(new FileModelSource(new File(getClass()
                .getResource("/poms/depmgmt/root-distance-explicit.xml")
                .getFile())));
        request.setModelResolver(new BaseModelResolver() {
            public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                    throws UnresolvableModelException {
                switch (dependency.getManagementKey()) {
                    case "org.junit:bom:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/junit-" + dependency.getVersion() + ".xml")
                                .getFile()));
                    case "test:other:pom":
                        return new FileModelSource(new File(getClass()
                                .getResource("/poms/depmgmt/distant-import.xml")
                                .getFile()));
                    default:
                        throw new UnresolvableModelException(
                                "Cannot resolve",
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion());
                }
            }
        });

        ModelBuildingResult result = builder.build(request);
        Dependency dep = result.getEffectiveModel().getDelegate().getDependencyManagement().getDependencies().stream()
                .filter(d -> "org.junit:junit:jar".equals(d.getManagementKey()))
                .findFirst()
                .get();
        assertEquals("0.3", dep.getVersion());
        assertEquals(0, result.getProblems().size());
    }
}
