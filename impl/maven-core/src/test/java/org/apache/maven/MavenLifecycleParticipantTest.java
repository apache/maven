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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.spi.LifecycleParticipant;
import org.apache.maven.api.spi.ProjectExecutionEvent;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class MavenLifecycleParticipantTest extends AbstractCoreMavenComponentTestCase {

    private static final String INJECTED_ARTIFACT_ID = "injected";

    public static class InjectDependencyLifecycleListener extends AbstractMavenLifecycleParticipant {

        @Override
        public void afterProjectsRead(MavenSession session) {
            MavenProject project = session.getProjects().get(0);

            Dependency dependency = new Dependency();
            dependency.setArtifactId(INJECTED_ARTIFACT_ID);
            dependency.setGroupId("foo");
            dependency.setVersion("1.2.3");
            dependency.setScope("system");
            try {
                dependency.setSystemPath(new File(
                                "src/test/projects/lifecycle-executor/project-with-additional-lifecycle-elements/pom.xml")
                        .getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            project.getModel().addDependency(dependency);
        }

        @Override
        public void afterSessionStart(MavenSession session) {
            session.getUserProperties().setProperty("injected", "bar");
        }
    }

    public static class InjectReactorDependency extends AbstractMavenLifecycleParticipant {
        @Override
        public void afterProjectsRead(MavenSession session) {
            injectReactorDependency(session.getProjects(), "module-a", "module-b");
        }

        private void injectReactorDependency(List<MavenProject> projects, String moduleFrom, String moduleTo) {
            for (MavenProject project : projects) {
                if (moduleFrom.equals(project.getArtifactId())) {
                    Dependency dependency = new Dependency();
                    dependency.setArtifactId(moduleTo);
                    dependency.setGroupId(project.getGroupId());
                    dependency.setVersion(project.getVersion());

                    project.getModel().addDependency(dependency);
                }
            }
        }
    }

    public static class SelectCurrentProject extends AbstractMavenLifecycleParticipant {
        @Override
        public void afterProjectsRead(MavenSession session) {
            assertEquals("dependency", session.getProjects().get(0).getArtifactId());
            assertEquals("root", session.getTopLevelProject().getArtifactId());
            assertSame(session.getTopLevelProject(), session.getCurrentProject());
            session.setProjects(List.of(session.getCurrentProject()));
        }
    }

    public static class InjectReactorDependencyV4 implements LifecycleParticipant {
        @Override
        public void afterProjectsRead(Session session) {
            ProjectManager projectManager = session.getService(ProjectManager.class);
            Project target = session.getProjects().stream()
                    .filter(project -> "module-a".equals(project.artifactId()))
                    .findFirst()
                    .orElseThrow();
            Project dependency = session.getProjects().stream()
                    .filter(project -> "module-b".equals(project.artifactId()))
                    .findFirst()
                    .orElseThrow();

            List<org.apache.maven.api.DependencyCoordinates> dependencies = new ArrayList<>(target.dependencies());
            dependencies.add(session.createDependencyCoordinates(session.createArtifactCoordinates(
                    dependency.groupId(), dependency.artifactId(), dependency.version(), "", "jar", "jar")));
            projectManager.setDependencies(target, dependencies);
        }
    }

    public static class RecordingLifecycleParticipant implements LifecycleParticipant {
        private static final List<String> CALLBACKS = new CopyOnWriteArrayList<>();

        @Override
        public void afterProjectsRead(Session session) {
            CALLBACKS.add("afterProjectsRead:" + session.getProjects().size());
        }

        @Override
        public void beforeProjectExecution(ProjectExecutionEvent event) {
            CALLBACKS.add("before:" + event.project().artifactId() + ':' + goal(event));
        }

        @Override
        public void afterProjectExecution(ProjectExecutionEvent event) {
            CALLBACKS.add("after:" + event.project().artifactId() + ':' + goal(event) + ':'
                    + event.failure().isEmpty());
        }

        @Override
        public void afterSessionEnd(Session session) {
            CALLBACKS.add("afterSessionEnd");
        }

        private String goal(ProjectExecutionEvent event) {
            return event.executionPlan().isEmpty()
                    ? "(no plan)"
                    : event.executionPlan().get(0).goal();
        }
    }

    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/lifecycle-listener";
    }

    @Test
    void testDependencyInjection() throws Exception {
        PlexusContainer container = getContainer();

        ComponentDescriptor<? extends AbstractMavenLifecycleParticipant> cd =
                new ComponentDescriptor<>(InjectDependencyLifecycleListener.class, container.getContainerRealm());
        cd.setRoleClass(AbstractMavenLifecycleParticipant.class);
        container.addComponentDescriptor(cd);

        Maven maven = container.lookup(Maven.class);
        File pom = getProject("lifecycle-listener-dependency-injection");
        MavenExecutionRequest request = createMavenExecutionRequest(pom);
        request.setGoals(Arrays.asList("validate"));
        MavenExecutionResult result = maven.execute(request);

        assertFalse(result.hasExceptions(), result.getExceptions().toString());

        MavenProject project = result.getProject();

        assertEquals("bar", project.getProperties().getProperty("foo"));

        ArrayList<Artifact> artifacts = new ArrayList<>(project.getArtifacts());

        assertEquals(1, artifacts.size());
        assertEquals(INJECTED_ARTIFACT_ID, artifacts.get(0).getArtifactId());
    }

    @Test
    void testReactorDependencyInjection() throws Exception {
        List<String> reactorOrder =
                getReactorOrder("lifecycle-participant-reactor-dependency-injection", InjectReactorDependency.class);
        assertEquals(Arrays.asList("parent", "module-b", "module-a"), reactorOrder);
    }

    @Test
    void testCurrentProjectSelection() throws Exception {
        List<String> reactorOrder = getReactorOrder("current-project-selection", SelectCurrentProject.class);
        assertEquals(List.of("root"), reactorOrder);
    }

    @Test
    void testV4ReactorDependencyInjection() throws Exception {
        List<String> reactorOrder = getV4ReactorOrder(
                "lifecycle-participant-reactor-dependency-injection", InjectReactorDependencyV4.class);
        assertEquals(Arrays.asList("parent", "module-b", "module-a"), reactorOrder);
    }

    @Test
    void testV4LifecycleCallbacks() throws Exception {
        RecordingLifecycleParticipant.CALLBACKS.clear();
        PlexusContainer container = getContainer();
        ComponentDescriptor<RecordingLifecycleParticipant> cd =
                new ComponentDescriptor<>(RecordingLifecycleParticipant.class, container.getContainerRealm());
        cd.setRoleClass(LifecycleParticipant.class);
        container.addComponentDescriptor(cd);

        Maven maven = container.lookup(Maven.class);
        MavenExecutionRequest request =
                createMavenExecutionRequest(getProject("lifecycle-listener-dependency-injection"));
        request.setGoals(List.of("validate"));
        MavenExecutionResult result = maven.execute(request);

        assertFalse(result.hasExceptions(), result.getExceptions().toString());
        assertEquals(
                List.of("afterProjectsRead:1", "before:simple:it", "after:simple:it:true", "afterSessionEnd"),
                RecordingLifecycleParticipant.CALLBACKS);
    }

    private <T> List<String> getReactorOrder(String testProject, Class<T> participant) throws Exception {
        PlexusContainer container = getContainer();

        ComponentDescriptor<T> cd = new ComponentDescriptor<>(participant, container.getContainerRealm());
        cd.setRoleClass(AbstractMavenLifecycleParticipant.class);
        container.addComponentDescriptor(cd);

        Maven maven = container.lookup(Maven.class);
        File pom = getProject(testProject);
        MavenExecutionRequest request = createMavenExecutionRequest(pom);
        request.setGoals(Arrays.asList("validate"));
        MavenExecutionResult result = maven.execute(request);

        assertFalse(result.hasExceptions(), result.getExceptions().toString());

        List<String> order = new ArrayList<>();
        for (MavenProject project : result.getTopologicallySortedProjects()) {
            order.add(project.getArtifactId());
        }
        return order;
    }

    private <T extends LifecycleParticipant> List<String> getV4ReactorOrder(String testProject, Class<T> participant)
            throws Exception {
        PlexusContainer container = getContainer();

        ComponentDescriptor<T> cd = new ComponentDescriptor<>(participant, container.getContainerRealm());
        cd.setRoleClass(LifecycleParticipant.class);
        container.addComponentDescriptor(cd);

        Maven maven = container.lookup(Maven.class);
        File pom = getProject(testProject);
        MavenExecutionRequest request = createMavenExecutionRequest(pom);
        request.setGoals(List.of("validate"));
        MavenExecutionResult result = maven.execute(request);

        assertFalse(result.hasExceptions(), result.getExceptions().toString());
        return result.getTopologicallySortedProjects().stream()
                .map(MavenProject::getArtifactId)
                .toList();
    }
}
