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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

public class MavenLifecycleParticipantTest extends AbstractCoreMavenComponentTestCase {

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

    @Override
    protected void setupContainer() {
        super.setupContainer();
    }

    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/lifecycle-listener";
    }

    public void testDependencyInjection() throws Exception {
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

        assertFalse(result.getExceptions().toString(), result.hasExceptions());

        MavenProject project = result.getProject();

        assertEquals("bar", project.getProperties().getProperty("foo"));

        ArrayList<Artifact> artifacts = new ArrayList<>(project.getArtifacts());

        assertEquals(1, artifacts.size());
        assertEquals(INJECTED_ARTIFACT_ID, artifacts.get(0).getArtifactId());
    }

    public void testReactorDependencyInjection() throws Exception {
        List<String> reactorOrder =
                getReactorOrder("lifecycle-participant-reactor-dependency-injection", InjectReactorDependency.class);
        assertEquals(Arrays.asList("parent", "module-b", "module-a"), reactorOrder);
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

        assertFalse(result.getExceptions().toString(), result.hasExceptions());

        List<String> order = new ArrayList<>();
        for (MavenProject project : result.getTopologicallySortedProjects()) {
            order.add(project.getArtifactId());
        }
        return order;
    }
}
