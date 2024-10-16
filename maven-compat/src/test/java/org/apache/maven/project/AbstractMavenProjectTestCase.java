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

import javax.inject.Inject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.apache.maven.api.Session;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultLookup;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.fail;

/**
 */
@PlexusTest
@Deprecated
public abstract class AbstractMavenProjectTestCase {
    protected ProjectBuilder projectBuilder;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected org.eclipse.aether.RepositorySystem resolverRepositorySystem;

    @Inject
    protected MavenRepositorySystem mavenRepositorySystem;

    @Inject
    protected PlexusContainer container;

    public PlexusContainer getContainer() {
        return container;
    }

    @BeforeEach
    public void setUp() throws Exception {
        if (getContainer().hasComponent(ProjectBuilder.class, "test")) {
            projectBuilder = getContainer().lookup(ProjectBuilder.class, "test");
        } else {
            // default over to the main project builder...
            projectBuilder = getContainer().lookup(ProjectBuilder.class);
        }
    }

    protected ProjectBuilder getProjectBuilder() {
        return projectBuilder;
    }

    // ----------------------------------------------------------------------
    // Local repository
    // ----------------------------------------------------------------------

    protected File getLocalRepositoryPath() throws FileNotFoundException, URISyntaxException {
        File markerFile = getFileForClasspathResource("local-repo/marker.txt");

        return markerFile.getAbsoluteFile().getParentFile();
    }

    protected static File getFileForClasspathResource(String resource)
            throws FileNotFoundException, URISyntaxException {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();

        URL resourceUrl = cloader.getResource(resource);

        if (resourceUrl == null) {
            throw new FileNotFoundException("Unable to find: " + resource);
        }

        return new File(resourceUrl.toURI());
    }

    protected ArtifactRepository getLocalRepository() throws Exception {
        ArtifactRepositoryLayout repoLayout = getContainer().lookup(ArtifactRepositoryLayout.class);

        ArtifactRepository r = repositorySystem.createArtifactRepository(
                "local", "file://" + getLocalRepositoryPath().getAbsolutePath(), repoLayout, null, null);

        return r;
    }

    // ----------------------------------------------------------------------
    // Project building
    // ----------------------------------------------------------------------

    protected MavenProject getProjectWithDependencies(File pom) throws Exception {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository(getLocalRepository());
        configuration.setRemoteRepositories(Arrays.asList(new ArtifactRepository[] {}));
        configuration.setProcessPlugins(true);
        configuration.setResolveDependencies(true);
        initRepoSession(configuration);

        try {
            return projectBuilder.build(pom, configuration).getProject();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof ModelBuildingException) {
                String message = "In: " + pom + "\n\n";
                for (ModelProblem problem : ((ModelBuildingException) cause).getProblems()) {
                    message += problem + "\n";
                }
                System.out.println(message);
                fail(message);
            }

            throw e;
        }
    }

    protected MavenProject getProject(File pom) throws Exception {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository(getLocalRepository());
        initRepoSession(configuration);

        return projectBuilder.build(pom, configuration).getProject();
    }

    protected void initRepoSession(ProjectBuildingRequest request) throws Exception {
        File localRepo = new File(request.getLocalRepository().getBasedir());
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(new LegacyLocalRepositoryManager(localRepo));
        request.setRepositorySession(session);

        DefaultMavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest(true);
        MavenSession msession =
                new MavenSession(getContainer(), session, mavenExecutionRequest, new DefaultMavenExecutionResult());
        DefaultSession iSession = new DefaultSession(
                msession, resolverRepositorySystem, null, mavenRepositorySystem, new DefaultLookup(container), null);
        InternalSession.associate(session, iSession);

        SessionScope sessionScope = container.lookup(SessionScope.class);
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, msession);
        sessionScope.seed(InternalMavenSession.class, iSession);
        sessionScope.seed(Session.class, iSession);
    }
}
