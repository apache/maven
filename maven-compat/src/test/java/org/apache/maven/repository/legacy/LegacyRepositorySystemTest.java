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

import java.io.File;
import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Tests {@link LegacyRepositorySystem}.
 *
 * @author Benjamin Bentmann
 */
public class LegacyRepositorySystemTest extends PlexusTestCase {
    private RepositorySystem repositorySystem;

    @Override
    protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
        super.customizeContainerConfiguration(containerConfiguration);
        containerConfiguration.setAutoWiring(true);
        containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repositorySystem = lookup(RepositorySystem.class, "default");
    }

    @Override
    protected void tearDown() throws Exception {
        repositorySystem = null;
        super.tearDown();
    }

    public void testThatLocalRepositoryWithSpacesIsProperlyHandled() throws Exception {
        File basedir = new File("target/spacy path").getAbsoluteFile();
        ArtifactRepository repo = repositorySystem.createLocalRepository(basedir);
        assertEquals(basedir, new File(repo.getBasedir()));
    }

    public void testAuthenticationHandling() throws Exception {
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
