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
package org.apache.maven;

import javax.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.Session;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultLookup;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.internal.impl.resolver.MavenSessionBuilderSupplier;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;

@PlexusTest
public abstract class AbstractCoreMavenComponentTestCase {

    @Inject
    protected PlexusContainer container;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected MavenRepositorySystem mavenRepositorySystem;

    @Inject
    protected org.apache.maven.project.ProjectBuilder projectBuilder;

    protected abstract String getProjectsDirectory();

    protected PlexusContainer getContainer() {
        return container;
    }

    protected File getProject(String name) throws Exception {
        File source = new File(new File(getBasedir(), getProjectsDirectory()), name);
        File target = new File(new File(getBasedir(), "target"), name);
        FileUtils.copyDirectoryStructureIfModified(source, target);
        return new File(target, "pom.xml");
    }

    protected MavenExecutionRequest createMavenExecutionRequest(File pom) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setRootDirectory(pom != null ? pom.toPath().getParent() : null)
                .setPom(pom)
                .setProjectPresent(true)
                .setShowErrors(true)
                .setPluginGroups(Arrays.asList("org.apache.maven.plugins"))
                .setLocalRepository(getLocalRepository())
                .setRemoteRepositories(getRemoteRepositories())
                .setPluginArtifactRepositories(getPluginArtifactRepositories())
                .setGoals(Arrays.asList("package"));

        if (pom != null) {
            request.setMultiModuleProjectDirectory(pom.getParentFile());
        }

        return request;
    }

    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession(File pom) throws Exception {
        return createMavenSession(pom, new Properties());
    }

    protected MavenSession createMavenSession(File pom, Properties executionProperties) throws Exception {
        return createMavenSession(pom, executionProperties, false);
    }

    protected MavenSession createMavenSession(File pom, Properties executionProperties, boolean includeModules)
            throws Exception {
        MavenExecutionRequest request = createMavenExecutionRequest(pom);

        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest()
                .setLocalRepository(request.getLocalRepository())
                .setRemoteRepositories(request.getRemoteRepositories())
                .setPluginArtifactRepositories(request.getPluginArtifactRepositories())
                .setSystemProperties(executionProperties)
                .setUserProperties(new Properties());

        initRepoSession(request, configuration);

        List<MavenProject> projects = new ArrayList<>();

        if (pom != null) {
            MavenProject project = projectBuilder.build(pom, configuration).getProject();

            projects.add(project);
            if (includeModules) {
                for (String module : project.getModules()) {
                    File modulePom = new File(pom.getParentFile(), module);
                    if (modulePom.isDirectory()) {
                        modulePom = new File(modulePom, "pom.xml");
                    }
                    projects.add(projectBuilder.build(modulePom, configuration).getProject());
                }
            }
        } else {
            MavenProject project = createStubMavenProject();
            project.setRemoteArtifactRepositories(request.getRemoteRepositories());
            project.setPluginArtifactRepositories(request.getPluginArtifactRepositories());
            projects.add(project);
        }

        InternalSession iSession = InternalSession.from(configuration.getRepositorySession());
        InternalMavenSession mSession = InternalMavenSession.from(iSession);
        MavenSession session = mSession.getMavenSession();

        session.setProjects(projects);
        session.setAllProjects(session.getProjects());

        return session;
    }

    protected void initRepoSession(
            MavenExecutionRequest mavenExecutionRequest, ProjectBuildingRequest projectBuildingRequest)
            throws Exception {
        File localRepoDir = new File(projectBuildingRequest.getLocalRepository().getBasedir());
        LocalRepository localRepo = new LocalRepository(localRepoDir, "simple");

        RepositorySystemSession session = new MavenSessionBuilderSupplier(repositorySystem)
                .get()
                .withLocalRepositories(localRepo)
                .build();
        projectBuildingRequest.setRepositorySession(session);

        DefaultSessionFactory defaultSessionFactory =
                new DefaultSessionFactory(repositorySystem, null, new DefaultLookup(container), null);

        MavenSession mSession = new MavenSession(
                container,
                projectBuildingRequest.getRepositorySession(),
                mavenExecutionRequest,
                new DefaultMavenExecutionResult());

        InternalSession iSession = defaultSessionFactory.newSession(mSession);
        mSession.setSession(iSession);

        SessionScope sessionScope = getContainer().lookup(SessionScope.class);
        sessionScope.enter();
        sessionScope.seed(MavenSession.class, mSession);
        sessionScope.seed(Session.class, iSession);
        sessionScope.seed(InternalMavenSession.class, InternalMavenSession.from(iSession));
    }

    protected MavenProject createStubMavenProject() {
        Model model = new Model();
        model.setGroupId("org.apache.maven.test");
        model.setArtifactId("maven-test");
        model.setVersion("1.0");
        return new MavenProject(model);
    }

    protected List<ArtifactRepository> getRemoteRepositories() throws InvalidRepositoryException {
        File repoDir = new File(getBasedir(), "src/test/remote-repo").getAbsoluteFile();

        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setChecksumPolicy("ignore");
        policy.setUpdatePolicy("always");

        Repository repository = new Repository();
        repository.setId(MavenRepositorySystem.DEFAULT_REMOTE_REPO_ID);
        repository.setUrl("file://" + repoDir.toURI().getPath());
        repository.setReleases(policy);
        repository.setSnapshots(policy);

        return Arrays.asList(mavenRepositorySystem.buildArtifactRepository(repository));
    }

    protected List<ArtifactRepository> getPluginArtifactRepositories() throws InvalidRepositoryException {
        return getRemoteRepositories();
    }

    protected ArtifactRepository getLocalRepository() throws InvalidRepositoryException {
        File repoDir = new File(getBasedir(), "target/local-repo").getAbsoluteFile();

        return mavenRepositorySystem.createLocalRepository(repoDir);
    }

    protected class ProjectBuilder {
        private MavenProject project;

        public ProjectBuilder(MavenProject project) {
            this.project = project;
        }

        public ProjectBuilder(String groupId, String artifactId, String version) {
            Model model = new Model();
            model.setModelVersion("4.0.0");
            model.setGroupId(groupId);
            model.setArtifactId(artifactId);
            model.setVersion(version);
            model.setBuild(new Build());
            project = new MavenProject(model);
        }

        public ProjectBuilder setGroupId(String groupId) {
            project.setGroupId(groupId);
            return this;
        }

        public ProjectBuilder setArtifactId(String artifactId) {
            project.setArtifactId(artifactId);
            return this;
        }

        public ProjectBuilder setVersion(String version) {
            project.setVersion(version);
            return this;
        }

        // Dependencies
        //
        public ProjectBuilder addDependency(String groupId, String artifactId, String version, String scope) {
            return addDependency(groupId, artifactId, version, scope, (Exclusion) null);
        }

        public ProjectBuilder addDependency(
                String groupId, String artifactId, String version, String scope, Exclusion exclusion) {
            return addDependency(groupId, artifactId, version, scope, null, exclusion);
        }

        public ProjectBuilder addDependency(
                String groupId, String artifactId, String version, String scope, String systemPath) {
            return addDependency(groupId, artifactId, version, scope, systemPath, null);
        }

        public ProjectBuilder addDependency(
                String groupId,
                String artifactId,
                String version,
                String scope,
                String systemPath,
                Exclusion exclusion) {
            Dependency d = new Dependency();
            d.setGroupId(groupId);
            d.setArtifactId(artifactId);
            d.setVersion(version);
            d.setScope(scope);

            if (systemPath != null && scope.equals(Artifact.SCOPE_SYSTEM)) {
                d.setSystemPath(systemPath);
            }

            if (exclusion != null) {
                d.addExclusion(exclusion);
            }

            project.getDependencies().add(d);

            return this;
        }

        // Plugins
        //
        public ProjectBuilder addPlugin(Plugin plugin) {
            project.getBuildPlugins().add(plugin);
            return this;
        }

        public MavenProject get() {
            return project;
        }
    }
}
