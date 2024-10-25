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
package org.apache.maven.classrealm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.internal.impl.internal.DefaultCoreRealm;
import org.apache.maven.model.Model;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.artifact.Artifact;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * @author Sebastien Doyon
 */
class DefaultClassRealmManagerTest {

    private DefaultClassRealmManager newDefaultClassRealmManager(PlexusContainer container) {
        HashSet<String> exportedPackages = new HashSet<String>();
        exportedPackages.add("group1:artifact1");

        return new DefaultClassRealmManager(
                new DefaultCoreRealm(container),
                new ArrayList<ClassRealmManagerDelegate>(),
                new CoreExports(new ClassRealm(null, "test", null), new HashSet<String>(), exportedPackages));
    }

    private List<Artifact> newTestArtifactList() {
        List<Artifact> artifacts = new ArrayList<Artifact>();

        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(new File(new File("local/repository"), "some/path"));
        when(artifact.getGroupId()).thenReturn("group1");
        when(artifact.getArtifactId()).thenReturn("artifact1");
        when(artifact.getExtension()).thenReturn("ext");
        when(artifact.getClassifier()).thenReturn("classifier1");
        when(artifact.getVersion()).thenReturn("1");
        artifacts.add(artifact);

        Artifact artifact2 = mock(Artifact.class);
        when(artifact2.getFile()).thenReturn(null);
        when(artifact2.getGroupId()).thenReturn("group1");
        when(artifact2.getArtifactId()).thenReturn("artifact2");
        when(artifact2.getExtension()).thenReturn("ext");
        when(artifact2.getClassifier()).thenReturn("classifier1");
        when(artifact2.getVersion()).thenReturn("1");
        artifacts.add(artifact2);

        return artifacts;
    }

    private Model newTestModel() {
        Model model = new Model();
        model.setGroupId("modelGroup1");
        model.setArtifactId("modelArtifact1");
        model.setVersion("modelVersion1");

        return model;
    }

    @Test
    void testDebugEnabled() throws PlexusContainerException {
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);

        DefaultClassRealmManager classRealmManager;
        ClassRealm classRealm;

        InOrder verifier = inOrder(logger);

        PlexusContainer container = new DefaultPlexusContainer();

        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            mockedLoggerFactory
                    .when(() -> LoggerFactory.getLogger(DefaultClassRealmManager.class))
                    .thenReturn(logger);

            classRealmManager = newDefaultClassRealmManager(container);
            classRealm = classRealmManager.createProjectRealm(newTestModel(), newTestArtifactList());
        }

        assertEquals(classRealmManager.getMavenApiRealm(), classRealm.getParentClassLoader());
        assertEquals("project>modelGroup1:modelArtifact1:modelVersion1", classRealm.getId());
        assertEquals(1, classRealm.getURLs().length);
        assertThat(classRealm.getURLs()[0].getPath(), endsWith("local/repository/some/path"));

        verifier.verify(logger, calls(1)).debug("Importing foreign packages into class realm {}", "maven.api");
        verifier.verify(logger, calls(1)).debug("  Imported: {} < {}", "group1:artifact1", "test");
        verifier.verify(logger, calls(1)).debug("  Excluded: {}", "group1:artifact2:ext:classifier1:null");
        verifier.verify(logger, calls(1))
                .debug("Populating class realm {}", "project>modelGroup1:modelArtifact1:modelVersion1");
        verifier.verify(logger, calls(1)).debug("  Included: {}", "group1:artifact1:ext:classifier1:null");
    }

    @Test
    void testDebugDisabled() throws PlexusContainerException {
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(false);

        DefaultClassRealmManager classRealmManager;
        ClassRealm classRealm;

        InOrder verifier = inOrder(logger);

        PlexusContainer container = new DefaultPlexusContainer();

        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            mockedLoggerFactory
                    .when(() -> LoggerFactory.getLogger(DefaultClassRealmManager.class))
                    .thenReturn(logger);

            classRealmManager = newDefaultClassRealmManager(container);
            classRealm = classRealmManager.createProjectRealm(newTestModel(), newTestArtifactList());
        }

        assertEquals(classRealmManager.getMavenApiRealm(), classRealm.getParentClassLoader());
        assertEquals("project>modelGroup1:modelArtifact1:modelVersion1", classRealm.getId());
        assertEquals(1, classRealm.getURLs().length);
        assertThat(classRealm.getURLs()[0].getPath(), endsWith("local/repository/some/path"));

        verifier.verify(logger, calls(1)).debug("Importing foreign packages into class realm {}", "maven.api");
        verifier.verify(logger, calls(1)).debug("  Imported: {} < {}", "group1:artifact1", "test");
        verifier.verify(logger, calls(1))
                .debug("Populating class realm {}", "project>modelGroup1:modelArtifact1:modelVersion1");
        verifier.verify(logger, never()).debug("  Included: {}", "group1:artifact1:ext:classifier1:null");
        verifier.verify(logger, never()).debug("  Excluded: {}", "group1:artifact2:ext:classifier1:null");
    }
}
