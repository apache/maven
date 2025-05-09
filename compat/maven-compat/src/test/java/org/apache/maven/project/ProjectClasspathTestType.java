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

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.impl.resolver.DefaultArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Deprecated
class ProjectClasspathTestType extends AbstractMavenProjectTestCase {
    static final String DIR = "projects/scope/";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        ArtifactResolver resolver = getContainer().lookup(ArtifactResolver.class, "classpath");
        DefaultArtifactDescriptorReader pomReader =
                (DefaultArtifactDescriptorReader) getContainer().lookup(ArtifactDescriptorReader.class);
        Field field = DefaultArtifactDescriptorReader.class.getDeclaredField("artifactResolver");
        field.setAccessible(true);
        field.set(pomReader, resolver);

        projectBuilder = getContainer().lookup(ProjectBuilder.class, "classpath");
    }

    @Test
    void projectClasspath() throws Exception {
        File f = getFileForClasspathResource(DIR + "project-with-scoped-dependencies.xml");

        MavenProject project = getProjectWithDependencies(f);

        Artifact artifact;

        assertThat(project).as("Test project can't be null!").isNotNull();

        checkArtifactIdScope(project, "provided", "provided");
        checkArtifactIdScope(project, "test", "test");
        checkArtifactIdScope(project, "compile", "compile");
        checkArtifactIdScope(project, "runtime", "runtime");
        checkArtifactIdScope(project, "default", "compile");

        // check all transitive deps of a test dependency are test, except test and provided which is skipped
        artifact = getArtifact(project, "maven-test-test", "scope-provided");
        assertThat(artifact).as("Check no provided dependencies are transitive").isNull();
        artifact = getArtifact(project, "maven-test-test", "scope-test");
        assertThat(artifact).as("Check no test dependencies are transitive").isNull();

        artifact = getArtifact(project, "maven-test-test", "scope-compile");
        assertThat(artifact).isNotNull();

        System.out.println("a = " + artifact);
        System.out.println("b = " + artifact.getScope());
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("test");
        artifact = getArtifact(project, "maven-test-test", "scope-default");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("test");
        artifact = getArtifact(project, "maven-test-test", "scope-runtime");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("test");

        // check all transitive deps of a provided dependency are provided scope, except for test
        checkGroupIdScope(project, "provided", "maven-test-provided");
        artifact = getArtifact(project, "maven-test-provided", "scope-runtime");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("provided");

        // check all transitive deps of a runtime dependency are runtime scope, except for test
        checkGroupIdScope(project, "runtime", "maven-test-runtime");
        artifact = getArtifact(project, "maven-test-runtime", "scope-runtime");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("runtime");

        // check all transitive deps of a compile dependency are compile scope, except for runtime and test
        checkGroupIdScope(project, "compile", "maven-test-compile");
        artifact = getArtifact(project, "maven-test-compile", "scope-runtime");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("runtime");

        // check all transitive deps of a default dependency are compile scope, except for runtime and test
        checkGroupIdScope(project, "compile", "maven-test-default");
        artifact = getArtifact(project, "maven-test-default", "scope-runtime");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo("runtime");
    }

    private void checkGroupIdScope(MavenProject project, String scopeValue, String groupId) {
        Artifact artifact;
        artifact = getArtifact(project, groupId, "scope-compile");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo(scopeValue);
        artifact = getArtifact(project, groupId, "scope-test");
        assertThat(artifact).as("Check test dependency is not transitive").isNull();
        artifact = getArtifact(project, groupId, "scope-provided");
        assertThat(artifact).as("Check provided dependency is not transitive").isNull();
        artifact = getArtifact(project, groupId, "scope-default");
        assertThat(artifact.getScope()).as("Check scope").isEqualTo(scopeValue);
    }

    private void checkArtifactIdScope(MavenProject project, String scope, String scopeValue) {
        String artifactId = "scope-" + scope;
        Artifact artifact = getArtifact(project, "maven-test", artifactId);
        assertThat(artifact).isNotNull();
        assertThat(artifact.getScope()).as("Check scope").isEqualTo(scopeValue);
    }

    private Artifact getArtifact(MavenProject project, String groupId, String artifactId) {
        System.out.println("[ Looking for " + groupId + ":" + artifactId + " ]");
        for (Artifact a : project.getArtifacts()) {
            System.out.println(a.toString());
            if (artifactId.equals(a.getArtifactId()) && a.getGroupId().equals(groupId)) {
                System.out.println("RETURN");
                return a;
            }
        }
        System.out.println("Return null");
        return null;
    }
}
