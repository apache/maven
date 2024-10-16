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
package org.apache.maven.repository.legacy;

import javax.inject.Inject;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests {@link LegacyRepositorySystem}.
 *
 */
@PlexusTest
@Deprecated
class LegacyRepositorySystemTest {
    @Inject
    private LegacyRepositorySystem repositorySystem;

    @Test
    void testThatLocalRepositoryWithSpacesIsProperlyHandled() throws Exception {
        File basedir = new File("target/spacy path").getAbsoluteFile();
        ArtifactRepository repo = repositorySystem.createLocalRepository(basedir);
        assertEquals(basedir, new File(repo.getBasedir()));
    }

    @Test
    void testAuthenticationHandling() {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");

        ArtifactRepository repository =
                repositorySystem.createArtifactRepository("repository", "http://foo", null, null, null);
        repositorySystem.injectAuthentication(Arrays.asList(repository), Arrays.asList(server));
        Authentication authentication = repository.getAuthentication();
        assertNotNull(authentication);
        assertEquals("jason", authentication.getUsername());
        assertEquals("abc123", authentication.getPassword());
    }
}
