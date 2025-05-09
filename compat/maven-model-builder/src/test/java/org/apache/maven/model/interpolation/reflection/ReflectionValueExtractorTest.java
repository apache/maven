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
package org.apache.maven.model.interpolation.reflection;

/*
 * Copyright The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReflectionValueExtractorTest class.
 */
@Deprecated
class ReflectionValueExtractorTest {
    private Project project;

    /**
     * <p>setUp.</p>
     */
    @BeforeEach
    void setUp() {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId("dep1");
        Dependency dependency2 = new Dependency();
        dependency2.setArtifactId("dep2");

        project = new Project();
        project.setModelVersion("4.0.0");
        project.setGroupId("org.apache.maven");
        project.setArtifactId("maven-core");
        project.setName("Maven");
        project.setVersion("2.0-SNAPSHOT");
        project.setScm(new Scm());
        project.getScm().setConnection("scm-connection");
        project.addDependency(dependency1);
        project.addDependency(dependency2);
        project.setBuild(new Build());

        // Build up an artifactMap
        project.addArtifact(new Artifact("g0", "a0", "v0", "e0", "c0"));
        project.addArtifact(new Artifact("g1", "a1", "v1", "e1", "c1"));
        project.addArtifact(new Artifact("g2", "a2", "v2", "e2", "c2"));
    }

    /**
     * <p>testValueExtraction.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void valueExtraction() throws Exception {
        // ----------------------------------------------------------------------
        // Top level values
        // ----------------------------------------------------------------------

        assertThat(ReflectionValueExtractor.evaluate("project.modelVersion", project)).isEqualTo("4.0.0");

        assertThat(ReflectionValueExtractor.evaluate("project.groupId", project)).isEqualTo("org.apache.maven");

        assertThat(ReflectionValueExtractor.evaluate("project.artifactId", project)).isEqualTo("maven-core");

        assertThat(ReflectionValueExtractor.evaluate("project.name", project)).isEqualTo("Maven");

        assertThat(ReflectionValueExtractor.evaluate("project.version", project)).isEqualTo("2.0-SNAPSHOT");

        // ----------------------------------------------------------------------
        // SCM
        // ----------------------------------------------------------------------

        assertThat(ReflectionValueExtractor.evaluate("project.scm.connection", project)).isEqualTo("scm-connection");

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        List<?> dependencies = (List) ReflectionValueExtractor.evaluate("project.dependencies", project);

        assertThat(dependencies).isNotNull();

        assertThat(dependencies.size()).isEqualTo(2);

        // ----------------------------------------------------------------------
        // Dependencies - using index notation
        // ----------------------------------------------------------------------

        // List
        Dependency dependency = (Dependency) ReflectionValueExtractor.evaluate("project.dependencies[0]", project);

        assertThat(dependency).isNotNull();

        assertThat(dependency.getArtifactId()).isEqualTo("dep1");

        String artifactId = (String) ReflectionValueExtractor.evaluate("project.dependencies[1].artifactId", project);

        assertThat(artifactId).isEqualTo("dep2");

        // Array

        dependency = (Dependency) ReflectionValueExtractor.evaluate("project.dependenciesAsArray[0]", project);

        assertThat(dependency).isNotNull();

        assertThat(dependency.getArtifactId()).isEqualTo("dep1");

        artifactId = (String) ReflectionValueExtractor.evaluate("project.dependenciesAsArray[1].artifactId", project);

        assertThat(artifactId).isEqualTo("dep2");

        // Map

        dependency = (Dependency) ReflectionValueExtractor.evaluate("project.dependenciesAsMap(dep1)", project);

        assertThat(dependency).isNotNull();

        assertThat(dependency.getArtifactId()).isEqualTo("dep1");

        artifactId = (String) ReflectionValueExtractor.evaluate("project.dependenciesAsMap(dep2).artifactId", project);

        assertThat(artifactId).isEqualTo("dep2");

        // ----------------------------------------------------------------------
        // Build
        // ----------------------------------------------------------------------

        Build build = (Build) ReflectionValueExtractor.evaluate("project.build", project);

        assertThat(build).isNotNull();
    }

    /**
     * <p>testValueExtractorWithAInvalidExpression.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void valueExtractorWithAInvalidExpression() throws Exception {
        assertThat(ReflectionValueExtractor.evaluate("project.foo", project)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("project.dependencies[10]", project)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("project.dependencies[0].foo", project)).isNull();
    }

    /**
     * <p>testMappedDottedKey.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void mappedDottedKey() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("a.b", "a.b-value");

        assertThat(ReflectionValueExtractor.evaluate("h.value(a.b)", new ValueHolder(map))).isEqualTo("a.b-value");
    }

    /**
     * <p>testIndexedMapped.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void indexedMapped() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("a", "a-value");
        List<Object> list = new ArrayList<Object>();
        list.add(map);

        assertThat(ReflectionValueExtractor.evaluate("h.value[0](a)", new ValueHolder(list))).isEqualTo("a-value");
    }

    /**
     * <p>testMappedIndexed.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void mappedIndexed() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add("a-value");
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("a", list);
        assertThat(ReflectionValueExtractor.evaluate("h.value(a)[0]", new ValueHolder(map))).isEqualTo("a-value");
    }

    /**
     * <p>testMappedMissingDot.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void mappedMissingDot() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("a", new ValueHolder("a-value"));
        assertThat(ReflectionValueExtractor.evaluate("h.value(a)value", new ValueHolder(map))).isNull();
    }

    /**
     * <p>testIndexedMissingDot.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void indexedMissingDot() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add(new ValueHolder("a-value"));
        assertThat(ReflectionValueExtractor.evaluate("h.value[0]value", new ValueHolder(list))).isNull();
    }

    /**
     * <p>testDotDot.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void dotDot() throws Exception {
        assertThat(ReflectionValueExtractor.evaluate("h..value", new ValueHolder("value"))).isNull();
    }

    /**
     * <p>testBadIndexedSyntax.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void badIndexedSyntax() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add("a-value");
        Object value = new ValueHolder(list);

        assertThat(ReflectionValueExtractor.evaluate("h.value[", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value[]", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value[a]", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value[0", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value[0)", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value[-1]", value)).isNull();
    }

    /**
     * <p>testBadMappedSyntax.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void badMappedSyntax() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("a", "a-value");
        Object value = new ValueHolder(map);

        assertThat(ReflectionValueExtractor.evaluate("h.value(", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value()", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value(a", value)).isNull();
        assertThat(ReflectionValueExtractor.evaluate("h.value(a]", value)).isNull();
    }

    /**
     * <p>testIllegalIndexedType.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void illegalIndexedType() throws Exception {
        try {
            ReflectionValueExtractor.evaluate("h.value[1]", new ValueHolder("string"));
        } catch (Exception e) {
            // TODO assert exception message
        }
    }

    /**
     * <p>testIllegalMappedType.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void illegalMappedType() throws Exception {
        try {
            ReflectionValueExtractor.evaluate("h.value(key)", new ValueHolder("string"));
        } catch (Exception e) {
            // TODO assert exception message
        }
    }

    /**
     * <p>testTrimRootToken.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void trimRootToken() throws Exception {
        assertThat(ReflectionValueExtractor.evaluate("project", project, true)).isNull();
    }

    /**
     * <p>testArtifactMap.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void artifactMap() throws Exception {
        assertThat(((Artifact) ReflectionValueExtractor.evaluate("project.artifactMap(g0:a0:c0)", project)).getGroupId()).isEqualTo("g0");
        assertThat(((Artifact) ReflectionValueExtractor.evaluate("project.artifactMap(g1:a1:c1)", project))
                .getArtifactId()).isEqualTo("a1");
        assertThat(((Artifact) ReflectionValueExtractor.evaluate("project.artifactMap(g2:a2:c2)", project))
                .getClassifier()).isEqualTo("c2");
    }

    public static class Artifact {
        private String groupId;

        private String artifactId;

        private String version;

        private String extension;

        private String classifier;

        public Artifact(String groupId, String artifactId, String version, String extension, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.extension = extension;
            this.classifier = classifier;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }
    }

    public static class Project {
        private String modelVersion;

        private String groupId;

        private Scm scm;

        private List<Dependency> dependencies = new ArrayList<>();

        private Build build;

        private String artifactId;

        private String name;

        private String version;

        private Map<String, Artifact> artifactMap = new HashMap<>();
        private String description;

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public void setScm(Scm scm) {
            this.scm = scm;
        }

        public void addDependency(Dependency dependency) {
            this.dependencies.add(dependency);
        }

        public void setBuild(Build build) {
            this.build = build;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Scm getScm() {
            return scm;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public String getGroupId() {
            return groupId;
        }

        public List<Dependency> getDependencies() {
            return dependencies;
        }

        public Build getBuild() {
            return build;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public Dependency[] getDependenciesAsArray() {
            return getDependencies().toArray(new Dependency[0]);
        }

        public Map<String, Dependency> getDependenciesAsMap() {
            Map<String, Dependency> ret = new HashMap<>();
            for (Dependency dep : getDependencies()) {
                ret.put(dep.getArtifactId(), dep);
            }
            return ret;
        }

        // ${project.artifactMap(g:a:v)}
        public void addArtifact(Artifact a) {
            artifactMap.put(a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getClassifier(), a);
        }

        public Map<String, Artifact> getArtifactMap() {
            return artifactMap;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Build {}

    public static class Dependency {
        private String artifactId;

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String id) {
            artifactId = id;
        }
    }

    public static class Scm {
        private String connection;

        public void setConnection(String connection) {
            this.connection = connection;
        }

        public String getConnection() {
            return connection;
        }
    }

    public static class ValueHolder {
        private final Object value;

        public ValueHolder(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    /**
     * <p>testRootPropertyRegression.</p>
     *
     * @throws Exception if any.
     */
    @Test
    void rootPropertyRegression() throws Exception {
        Project project = new Project();
        project.setDescription("c:\\\\org\\apache\\test");
        Object evalued = ReflectionValueExtractor.evaluate("description", project);
        assertThat(evalued).isNotNull();
    }
}
