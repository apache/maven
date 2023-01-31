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

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Profile;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

public class PomConstructionWithSettingsTest extends PlexusTestCase {
    private static final String BASE_DIR = "src/test";

    private static final String BASE_POM_DIR = BASE_DIR + "/resources-settings";

    private DefaultProjectBuilder projectBuilder;

    private RepositorySystem repositorySystem;

    private File testDirectory;

    @Override
    protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
        super.customizeContainerConfiguration(containerConfiguration);
        containerConfiguration.setAutoWiring(true);
        containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    protected void setUp() throws Exception {
        testDirectory = new File(getBasedir(), BASE_POM_DIR);
        projectBuilder = (DefaultProjectBuilder) lookup(ProjectBuilder.class);
        repositorySystem = lookup(RepositorySystem.class);
    }

    @Override
    protected void tearDown() throws Exception {
        projectBuilder = null;

        super.tearDown();
    }

    public void testSettingsNoPom() throws Exception {
        PomTestWrapper pom = buildPom("settings-no-pom");
        assertEquals("local-profile-prop-value", pom.getValue("properties/local-profile-prop"));
    }

    /**
     * MNG-4107
     */
    public void testPomAndSettingsInterpolation() throws Exception {
        PomTestWrapper pom = buildPom("test-pom-and-settings-interpolation");
        assertEquals("applied", pom.getValue("properties/settingsProfile"));
        assertEquals("applied", pom.getValue("properties/pomProfile"));
        assertEquals("settings", pom.getValue("properties/pomVsSettings"));
        assertEquals("settings", pom.getValue("properties/pomVsSettingsInterpolated"));
    }

    /**
     * MNG-4107
     */
    public void testRepositories() throws Exception {
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

        DefaultRepositorySystemSession repoSession = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo =
                new LocalRepository(config.getLocalRepository().getBasedir());
        repoSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, localRepo));
        config.setRepositorySession(repoSession);

        return new PomTestWrapper(pomFile, projectBuilder.build(pomFile, config).getProject());
    }

    private static Settings readSettingsFile(File settingsFile) throws IOException, XmlPullParserException {
        Settings settings = null;

        try (Reader reader = ReaderFactory.newXmlReader(settingsFile)) {
            SettingsXpp3Reader modelReader = new SettingsXpp3Reader();

            settings = modelReader.read(reader);
        }
        return settings;
    }
}
