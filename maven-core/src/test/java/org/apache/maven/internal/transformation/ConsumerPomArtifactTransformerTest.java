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
package org.apache.maven.internal.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.assertj.XmlAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConsumerPomArtifactTransformerTest {
    @Test
    void transform() throws Exception {
        Path beforePomFile =
                Paths.get("src/test/resources/projects/transform/before.pom").toAbsolutePath();
        Path afterPomFile =
                Paths.get("src/test/resources/projects/transform/after.pom").toAbsolutePath();

        try (InputStream expected = Files.newInputStream(afterPomFile);
                InputStream result =
                        ConsumerPomArtifactTransformer.transform(beforePomFile, new NoTransformerContext())) {
            XmlAssert.assertThat(result).and(expected).areIdentical();
        }
    }

    @Test
    void injectTransformedArtifactsWithoutPomShouldNotInjectAnyArtifacts() throws IOException {
        MavenProject emptyProject = new MavenProject();

        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);
        when(sessionDataMock.get(any())).thenReturn(new NoTransformerContext());

        new ConsumerPomArtifactTransformer().injectTransformedArtifacts(emptyProject, systemSessionMock);

        assertThat(emptyProject.getAttachedArtifacts()).isEmpty();
    }

    private static class NoTransformerContext implements TransformerContext {
        @Override
        public String getUserProperty(String key) {
            return null;
        }

        @Override
        public Model getRawModel(String groupId, String artifactId) throws IllegalStateException {
            return null;
        }

        @Override
        public Model getRawModel(Path p) {
            return null;
        }
    }
}
