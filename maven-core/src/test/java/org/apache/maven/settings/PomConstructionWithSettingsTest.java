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
package org.apache.maven.settings;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.maven.MavenTestHelper;
import org.apache.maven.api.settings.InputSource;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.Profile;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.settings.v4.SettingsStaxReader;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;

@PlexusTest
class PomConstructionWithSettingsTest {
    private static final String BASE_DIR = "src/test";

    private static final String BASE_POM_DIR = BASE_DIR + "/resources-settings";

    @Inject
    private DefaultProjectBuilder projectBuilder;

    @Inject
    private MavenRepositorySystem repositorySystem;

    @Inject
    private PlexusContainer container;

    private File testDirectory;

    @BeforeEach
    void setUp() throws Exception {
        testDirectory = new File(getBasedir(), BASE_POM_DIR);
    }

    @Test
    void testSettingsNoPom() throws Exception {
        PomTestWrapper pom = buildPom("settings-no-pom");
        assertEquals("local-profile-prop-value", pom.getValue("properties/local-profile-prop"));
    }

    /**
     * MNG-4107
     */
    @Test
    void testPomAndSettingsInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("test-pom-and-settings-interpolation");
        assertEquals("applied", pom.getValue("properties/settingsProfile"));
        assertEquals("applied", pom.getValue("properties/pomProfile"));
        assertEquals("settings", pom.getValue("properties/pomVsSettings"));
        assertEquals("settings", pom.getValue("properties/pomVsSettingsInterpolated"));
    }

    /**
     * MNG-4107
     */
    @Test
    void testRepositories() throws Exception {
        PomTestWrapper pom = buildPom("repositories");
        assertEquals("maven-core-it-0", pom.getValue("repositories[1]/id"));
    }

    private PomTestWrapper buildPom(String pomPath) throws Exception {
        File pomFile = new File(testDirectory + File.separator + pomPath, "pom.xml");
        File settingsFile = new File(testDirectory + File.separator + pomPath, "settings.xml");
        Settings settings = readSettingsFile(settingsFile);

        ProjectBuildingRequest config = new DefaultProjectBuildingRequest();

        for (org.apache.maven.settings.Profile rawProfile : settings.getProfiles()) {
            Profile profile = SettingsUtils.convertFromSettingsProfile(rawProfile);
            config.addProfile(profile);
        }

        String localRepoUrl =
                System.getProperty("maven.repo.local", System.getProperty("user.home") + "/.m2/repository");
        localRepoUrl = "file://" + localRepoUrl;
        config.setLocalRepository(repositorySystem.createArtifactRepository(
                "local", localRepoUrl, new DefaultRepositoryLayout(), null, null));
        config.setActiveProfileIds(settings.getActiveProfiles());

        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        LocalRepository localRepo =
                new LocalRepository(config.getLocalRepository().getBasedir());
        repoSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, localRepo));
        config.setRepositorySession(repoSession);

        return new PomTestWrapper(pomFile, projectBuilder.build(pomFile, config).getProject());
    }

    private static Settings readSettingsFile(File settingsFile) throws IOException, XMLStreamException {
        try (InputStream is = Files.newInputStream(settingsFile.toPath())) {
            SettingsStaxReader reader = new SettingsStaxReader();
            InputSource source = new InputSource(settingsFile.toString());
            return new Settings(reader.read(is, true, source));
        }
    }
}
