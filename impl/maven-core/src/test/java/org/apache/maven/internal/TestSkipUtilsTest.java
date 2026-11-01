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
package org.apache.maven.internal;

import java.util.Collections;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestSkipUtilsTest {

    @Test
    void allInputsNullReturnsFalse() {
        assertFalse(TestSkipUtils.isTestSkip((MavenSession) null, null));
        assertFalse(TestSkipUtils.isTestSkip((RepositorySystemSession) null, null));
    }

    @Test
    void emptyPropertiesReturnsFalse() {
        MavenSession session = mock(MavenSession.class);
        Properties userProps = new Properties();
        Properties sysProps = new Properties();
        when(session.getUserProperties()).thenReturn(userProps);
        when(session.getSystemProperties()).thenReturn(sysProps);

        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        when(session.getRepositorySession()).thenReturn(repoSession);

        MavenProject project = new MavenProject();
        assertFalse(TestSkipUtils.isTestSkip(session, project));
        assertFalse(TestSkipUtils.isTestSkip(repoSession, project));
    }

    @Test
    void userPropertiesTrueReturnsTrue() {
        MavenSession session = mock(MavenSession.class);
        Properties userProps = new Properties();
        userProps.setProperty("maven.test.skip", "true");
        when(session.getUserProperties()).thenReturn(userProps);

        assertTrue(TestSkipUtils.isTestSkip(session, null));
    }

    @Test
    void userPropertiesEmptyStringReturnsTrue() {
        MavenSession session = mock(MavenSession.class);
        Properties userProps = new Properties();
        userProps.setProperty("maven.test.skip", "");
        when(session.getUserProperties()).thenReturn(userProps);

        assertTrue(TestSkipUtils.isTestSkip(session, null));
    }

    @Test
    void userPropertiesFalseReturnsFalse() {
        MavenSession session = mock(MavenSession.class);
        Properties userProps = new Properties();
        userProps.setProperty("maven.test.skip", "false");
        when(session.getUserProperties()).thenReturn(userProps);

        MavenProject project = new MavenProject();
        project.getProperties().setProperty("maven.test.skip", "true");

        // User property "false" takes precedence and stops further evaluation
        assertFalse(TestSkipUtils.isTestSkip(session, project));
    }

    @Test
    void systemPropertiesTrueReturnsTrue() {
        MavenSession session = mock(MavenSession.class);
        Properties sysProps = new Properties();
        sysProps.setProperty("maven.test.skip", "true");
        when(session.getSystemProperties()).thenReturn(sysProps);

        assertTrue(TestSkipUtils.isTestSkip(session, null));
    }

    @Test
    void systemPropertiesEmptyStringReturnsTrue() {
        MavenSession session = mock(MavenSession.class);
        Properties sysProps = new Properties();
        sysProps.setProperty("maven.test.skip", "");
        when(session.getSystemProperties()).thenReturn(sysProps);

        assertTrue(TestSkipUtils.isTestSkip(session, null));
    }

    @Test
    void repoSessionConfigPropertiesTrueReturnsTrue() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setConfigProperty("maven.test.skip", "true");

        assertTrue(TestSkipUtils.isTestSkip(repoSession, null));

        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(repoSession);
        assertTrue(TestSkipUtils.isTestSkip(session, null));
    }

    @Test
    void repoSessionConfigPropertiesEmptyStringReturnsTrue() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setConfigProperty("maven.test.skip", "");

        assertTrue(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void repoSessionUserPropertiesTrueReturnsTrue() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setUserProperties(Collections.singletonMap("maven.test.skip", "true"));

        assertTrue(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void repoSessionSystemPropertiesTrueReturnsTrue() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setSystemProperties(Collections.singletonMap("maven.test.skip", "true"));

        assertTrue(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void repoSessionUserPropertiesOverridesConfigProperties() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setUserProperties(Collections.singletonMap("maven.test.skip", "false"));
        repoSession.setConfigProperty("maven.test.skip", "true");

        // User property "false" must override config property "true"
        assertFalse(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void repoSessionUserPropertiesOverridesSystemProperties() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setUserProperties(Collections.singletonMap("maven.test.skip", "false"));
        repoSession.setSystemProperties(Collections.singletonMap("maven.test.skip", "true"));

        // User property "false" must override system property "true"
        assertFalse(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void repoSessionSystemPropertiesOverridesConfigProperties() {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        repoSession.setSystemProperties(Collections.singletonMap("maven.test.skip", "false"));
        repoSession.setConfigProperty("maven.test.skip", "true");

        // System property "false" must override config property "true"
        assertFalse(TestSkipUtils.isTestSkip(repoSession, null));
    }

    @Test
    void projectPropertiesTrueReturnsTrue() {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("maven.test.skip", "true");

        assertTrue(TestSkipUtils.isTestSkip((MavenSession) null, project));
        assertTrue(TestSkipUtils.isTestSkip((RepositorySystemSession) null, project));
    }

    @Test
    void projectPropertiesEmptyStringReturnsTrue() {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("maven.test.skip", "");

        assertTrue(TestSkipUtils.isTestSkip((MavenSession) null, project));
        assertTrue(TestSkipUtils.isTestSkip((RepositorySystemSession) null, project));
    }

    @Test
    void projectPropertiesFalseReturnsFalse() {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("maven.test.skip", "false");

        assertFalse(TestSkipUtils.isTestSkip((MavenSession) null, project));
        assertFalse(TestSkipUtils.isTestSkip((RepositorySystemSession) null, project));
    }

    @Test
    void testArtifactFilterExcludesTestScopedTestJar() {
        DependencyFilter filter = TestSkipUtils.testArtifactFilter();

        // 1. type=test-jar with scope=test -> excluded
        DefaultArtifact testJarArtifact = new DefaultArtifact(
                "org.example",
                "my-lib",
                null,
                "jar",
                "1.0",
                Collections.singletonMap(ArtifactProperties.TYPE, "test-jar"),
                (java.nio.file.Path) null);
        DependencyNode node1 = new DefaultDependencyNode(new Dependency(testJarArtifact, "test"));
        assertFalse(filter.accept(node1, Collections.emptyList()));

        // 2. classifier=tests, extension=jar with scope=test -> excluded
        DefaultArtifact testsClassifierArtifact = new DefaultArtifact("org.example", "my-lib", "tests", "jar", "1.0");
        DependencyNode node2 = new DefaultDependencyNode(new Dependency(testsClassifierArtifact, "test"));
        assertFalse(filter.accept(node2, Collections.emptyList()));
    }

    @Test
    void testArtifactFilterIncludesCompileScopedTestJar() {
        DependencyFilter filter = TestSkipUtils.testArtifactFilter();

        // test-jar artifact with scope=compile -> included (e.g. shared test harness)
        DefaultArtifact testJarArtifact = new DefaultArtifact(
                "org.example",
                "my-lib",
                null,
                "jar",
                "1.0",
                Collections.singletonMap(ArtifactProperties.TYPE, "test-jar"),
                (java.nio.file.Path) null);
        DependencyNode node = new DefaultDependencyNode(new Dependency(testJarArtifact, "compile"));
        assertTrue(filter.accept(node, Collections.emptyList()));
    }

    @Test
    void testArtifactFilterIncludesTestScopedNonTestJar() {
        DependencyFilter filter = TestSkipUtils.testArtifactFilter();

        // Standard jar with scope=test -> included
        DefaultArtifact standardJar = new DefaultArtifact("org.example", "junit-jupiter", null, "jar", "5.10.0");
        DependencyNode node = new DefaultDependencyNode(new Dependency(standardJar, "test"));
        assertTrue(filter.accept(node, Collections.emptyList()));
    }

    @Test
    void testArtifactFilterAcceptsNullDependencyOrArtifact() {
        DependencyFilter filter = TestSkipUtils.testArtifactFilter();

        DependencyNode emptyNode = new DefaultDependencyNode((Dependency) null);
        assertTrue(filter.accept(emptyNode, Collections.emptyList()));
    }
}
