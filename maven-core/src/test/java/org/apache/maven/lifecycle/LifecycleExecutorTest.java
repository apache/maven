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
package org.apache.maven.lifecycle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.internal.DefaultLifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.ExecutionPlanItem;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class LifecycleExecutorTest extends AbstractCoreMavenComponentTestCase {
    @Requirement
    private DefaultLifecycleExecutor lifecycleExecutor;

    @Requirement
    private DefaultLifecycleTaskSegmentCalculator lifeCycleTaskSegmentCalculator;

    @Requirement
    private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

    @Requirement
    private MojoDescriptorCreator mojoDescriptorCreator;

    protected void setUp() throws Exception {
        super.setUp();
        lifecycleExecutor = (DefaultLifecycleExecutor) lookup(LifecycleExecutor.class);
        lifeCycleTaskSegmentCalculator =
                (DefaultLifecycleTaskSegmentCalculator) lookup(LifecycleTaskSegmentCalculator.class);
        lifeCycleExecutionPlanCalculator = lookup(LifecycleExecutionPlanCalculator.class);
        mojoDescriptorCreator = lookup(MojoDescriptorCreator.class);
        lookup(ExceptionHandler.class);
    }

    @Override
    protected void tearDown() throws Exception {
        lifecycleExecutor = null;
        super.tearDown();
    }

    protected String getProjectsDirectory() {
        return "src/test/projects/lifecycle-executor";
    }

    // -----------------------------------------------------------------------------------------------
    // Tests which exercise the lifecycle executor when it is dealing with default lifecycle phases.
    // -----------------------------------------------------------------------------------------------

    public void testCalculationOfBuildPlanWithIndividualTaskWherePluginIsSpecifiedInThePom() throws Exception {
        // We are doing something like "mvn resources:resources" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        assertEquals("project-basic", session.getCurrentProject().getArtifactId());
        assertEquals("1.0", session.getCurrentProject().getVersion());
        List<MojoExecution> executionPlan = getExecutions(calculateExecutionPlan(session, "resources:resources"));
        assertEquals(1, executionPlan.size());
        MojoExecution mojoExecution = executionPlan.get(0);
        assertNotNull(mojoExecution);
        assertEquals(
                "org.apache.maven.plugins",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId());
        assertEquals(
                "maven-resources-plugin",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId());
        assertEquals(
                "0.1", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion());
    }

    public void testCalculationOfBuildPlanWithIndividualTaskOfTheCleanLifecycle() throws Exception {
        // We are doing something like "mvn clean:clean" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        assertEquals("project-basic", session.getCurrentProject().getArtifactId());
        assertEquals("1.0", session.getCurrentProject().getVersion());
        List<MojoExecution> executionPlan = getExecutions(calculateExecutionPlan(session, "clean"));
        assertEquals(1, executionPlan.size());
        MojoExecution mojoExecution = executionPlan.get(0);
        assertNotNull(mojoExecution);
        assertEquals(
                "org.apache.maven.plugins",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId());
        assertEquals(
                "maven-clean-plugin",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId());
        assertEquals(
                "0.1", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion());
    }

    public void testCalculationOfBuildPlanWithIndividualTaskOfTheCleanCleanGoal() throws Exception {
        // We are doing something like "mvn clean:clean" where no version is specified but this
        // project we are working on has the version specified in the POM so the version should come from there.
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        assertEquals("project-basic", session.getCurrentProject().getArtifactId());
        assertEquals("1.0", session.getCurrentProject().getVersion());
        List<MojoExecution> executionPlan = getExecutions(calculateExecutionPlan(session, "clean:clean"));
        assertEquals(1, executionPlan.size());
        MojoExecution mojoExecution = executionPlan.get(0);
        assertNotNull(mojoExecution);
        assertEquals(
                "org.apache.maven.plugins",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getGroupId());
        assertEquals(
                "maven-clean-plugin",
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactId());
        assertEquals(
                "0.1", mojoExecution.getMojoDescriptor().getPluginDescriptor().getVersion());
    }

    List<MojoExecution> getExecutions(MavenExecutionPlan mavenExecutionPlan) {
        List<MojoExecution> result = new ArrayList<>();
        for (ExecutionPlanItem executionPlanItem : mavenExecutionPlan) {
            result.add(executionPlanItem.getMojoExecution());
        }
        return result;
    }

    // We need to take in multiple lifecycles
    public void testCalculationOfBuildPlanTasksOfTheCleanLifecycleAndTheInstallLifecycle() throws Exception {
        File pom = getProject("project-with-additional-lifecycle-elements");
        MavenSession session = createMavenSession(pom);
        assertEquals(
                "project-with-additional-lifecycle-elements",
                session.getCurrentProject().getArtifactId());
        assertEquals("1.0", session.getCurrentProject().getVersion());
        List<MojoExecution> executionPlan = getExecutions(calculateExecutionPlan(session, "clean", "install"));

        // [01] clean:clean
        // [02] resources:resources
        // [03] compiler:compile
        // [04] it:generate-metadata
        // [05] resources:testResources
        // [06] compiler:testCompile
        // [07] it:generate-test-metadata
        // [08] surefire:test
        // [09] jar:jar
        // [10] install:install
        //
        assertEquals(10, executionPlan.size());

        assertEquals("clean:clean", executionPlan.get(0).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "resources:resources", executionPlan.get(1).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "compiler:compile", executionPlan.get(2).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "it:generate-metadata", executionPlan.get(3).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "resources:testResources",
                executionPlan.get(4).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "compiler:testCompile", executionPlan.get(5).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "it:generate-test-metadata",
                executionPlan.get(6).getMojoDescriptor().getFullGoalName());
        assertEquals("surefire:test", executionPlan.get(7).getMojoDescriptor().getFullGoalName());
        assertEquals("jar:jar", executionPlan.get(8).getMojoDescriptor().getFullGoalName());
        assertEquals("install:install", executionPlan.get(9).getMojoDescriptor().getFullGoalName());
    }

    // We need to take in multiple lifecycles
    public void testCalculationOfBuildPlanWithMultipleExecutionsOfModello() throws Exception {
        File pom = getProject("project-with-multiple-executions");
        MavenSession session = createMavenSession(pom);
        assertEquals(
                "project-with-multiple-executions", session.getCurrentProject().getArtifactId());
        assertEquals("1.0.1", session.getCurrentProject().getVersion());

        MavenExecutionPlan plan = calculateExecutionPlan(session, "clean", "install");

        List<MojoExecution> executions = getExecutions(plan);

        // [01] clean:clean
        // [02] modello:xpp3-writer
        // [03] modello:java
        // [04] modello:xpp3-reader
        // [05] modello:xpp3-writer
        // [06] modello:java
        // [07] modello:xpp3-reader
        // [08] plugin:descriptor
        // [09] resources:resources
        // [10] compiler:compile
        // [11] resources:testResources
        // [12] compiler:testCompile
        // [13] surefire:test
        // [14] jar:jar
        // [15] plugin:addPluginArtifactMetadata
        // [16] install:install
        //

        assertEquals(16, executions.size());

        assertEquals("clean:clean", executions.get(0).getMojoDescriptor().getFullGoalName());
        assertEquals("it:xpp3-writer", executions.get(1).getMojoDescriptor().getFullGoalName());
        assertEquals("it:java", executions.get(2).getMojoDescriptor().getFullGoalName());
        assertEquals("it:xpp3-reader", executions.get(3).getMojoDescriptor().getFullGoalName());
        assertEquals("it:xpp3-writer", executions.get(4).getMojoDescriptor().getFullGoalName());
        assertEquals("it:java", executions.get(5).getMojoDescriptor().getFullGoalName());
        assertEquals("it:xpp3-reader", executions.get(6).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "resources:resources", executions.get(7).getMojoDescriptor().getFullGoalName());
        assertEquals("compiler:compile", executions.get(8).getMojoDescriptor().getFullGoalName());
        assertEquals("plugin:descriptor", executions.get(9).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "resources:testResources",
                executions.get(10).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "compiler:testCompile", executions.get(11).getMojoDescriptor().getFullGoalName());
        assertEquals("surefire:test", executions.get(12).getMojoDescriptor().getFullGoalName());
        assertEquals("jar:jar", executions.get(13).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "plugin:addPluginArtifactMetadata",
                executions.get(14).getMojoDescriptor().getFullGoalName());
        assertEquals("install:install", executions.get(15).getMojoDescriptor().getFullGoalName());

        assertEquals(
                "src/main/mdo/remote-resources.mdo",
                new MojoExecutionXPathContainer(executions.get(1)).getValue("configuration/models[1]/model"));
        assertEquals(
                "src/main/mdo/supplemental-model.mdo",
                new MojoExecutionXPathContainer(executions.get(4)).getValue("configuration/models[1]/model"));
    }

    public void testLifecycleQueryingUsingADefaultLifecyclePhase() throws Exception {
        File pom = getProject("project-with-additional-lifecycle-elements");
        MavenSession session = createMavenSession(pom);
        assertEquals(
                "project-with-additional-lifecycle-elements",
                session.getCurrentProject().getArtifactId());
        assertEquals("1.0", session.getCurrentProject().getVersion());
        List<MojoExecution> executionPlan = getExecutions(calculateExecutionPlan(session, "package"));

        // [01] resources:resources
        // [02] compiler:compile
        // [03] it:generate-metadata
        // [04] resources:testResources
        // [05] compiler:testCompile
        // [06] plexus-component-metadata:generate-test-metadata
        // [07] surefire:test
        // [08] jar:jar
        //
        assertEquals(8, executionPlan.size());

        assertEquals(
                "resources:resources", executionPlan.get(0).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "compiler:compile", executionPlan.get(1).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "it:generate-metadata", executionPlan.get(2).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "resources:testResources",
                executionPlan.get(3).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "compiler:testCompile", executionPlan.get(4).getMojoDescriptor().getFullGoalName());
        assertEquals(
                "it:generate-test-metadata",
                executionPlan.get(5).getMojoDescriptor().getFullGoalName());
        assertEquals("surefire:test", executionPlan.get(6).getMojoDescriptor().getFullGoalName());
        assertEquals("jar:jar", executionPlan.get(7).getMojoDescriptor().getFullGoalName());
    }

    public void testLifecyclePluginsRetrievalForDefaultLifecycle() throws Exception {
        List<Plugin> plugins = new ArrayList<>(lifecycleExecutor.getPluginsBoundByDefaultToAllLifecycles("jar"));

        assertEquals(8, plugins.size());
    }

    public void testPluginConfigurationCreation() throws Exception {
        File pom = getProject("project-with-additional-lifecycle-elements");
        MavenSession session = createMavenSession(pom);
        MojoDescriptor mojoDescriptor = mojoDescriptorCreator.getMojoDescriptor(
                "org.apache.maven.its.plugins:maven-it-plugin:0.1:java", session, session.getCurrentProject());
        Xpp3Dom dom = MojoDescriptorCreator.convert(mojoDescriptor);
        System.out.println(dom);
    }

    MavenExecutionPlan calculateExecutionPlan(MavenSession session, String... tasks) throws Exception {
        List<TaskSegment> taskSegments =
                lifeCycleTaskSegmentCalculator.calculateTaskSegments(session, Arrays.asList(tasks));

        TaskSegment mergedSegment = new TaskSegment(false);

        for (TaskSegment taskSegment : taskSegments) {
            mergedSegment.getTasks().addAll(taskSegment.getTasks());
        }

        return lifeCycleExecutionPlanCalculator.calculateExecutionPlan(
                session, session.getCurrentProject(), mergedSegment.getTasks());
    }

    public void testInvalidGoalName() throws Exception {
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        try {
            getExecutions(calculateExecutionPlan(session, "resources:"));
            fail("expected a MojoNotFoundException");
        } catch (MojoNotFoundException e) {
            assertEquals("", e.getGoal());
        }

        try {
            getExecutions(calculateExecutionPlan(
                    session, "org.apache.maven.plugins:maven-resources-plugin:0.1:resources:toomany"));
            fail("expected a MojoNotFoundException");
        } catch (MojoNotFoundException e) {
            assertEquals("resources:toomany", e.getGoal());
        }
    }

    public void testPluginPrefixRetrieval() throws Exception {
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        Plugin plugin = mojoDescriptorCreator.findPluginForPrefix("resources", session);
        assertEquals("org.apache.maven.plugins", plugin.getGroupId());
        assertEquals("maven-resources-plugin", plugin.getArtifactId());
    }

    // Prefixes

    public void testFindingPluginPrefixforCleanClean() throws Exception {
        File pom = getProject("project-basic");
        MavenSession session = createMavenSession(pom);
        Plugin plugin = mojoDescriptorCreator.findPluginForPrefix("clean", session);
        assertNotNull(plugin);
    }

    public void testSetupMojoExecution() throws Exception {
        File pom = getProject("mojo-configuration");

        MavenSession session = createMavenSession(pom);

        LifecycleTask task = new LifecycleTask("generate-sources");
        MavenExecutionPlan executionPlan = lifeCycleExecutionPlanCalculator.calculateExecutionPlan(
                session, session.getCurrentProject(), Arrays.asList((Object) task), false);

        MojoExecution execution = executionPlan.getMojoExecutions().get(0);
        assertEquals(execution.toString(), "maven-it-plugin", execution.getArtifactId());
        assertNull(execution.getConfiguration());

        lifeCycleExecutionPlanCalculator.setupMojoExecution(session, session.getCurrentProject(), execution);
        assertNotNull(execution.getConfiguration());
        assertEquals("1.0", execution.getConfiguration().getChild("version").getAttribute("default-value"));
    }

    public void testExecutionListeners() throws Exception {
        final File pom = getProject("project-basic");
        final MavenSession session = createMavenSession(pom);
        session.setProjectDependencyGraph(new ProjectDependencyGraph() {
            public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
                return Collections.emptyList();
            }

            public List<MavenProject> getAllProjects() {
                return session.getAllProjects();
            }

            public List<MavenProject> getSortedProjects() {
                return Collections.singletonList(session.getCurrentProject());
            }

            public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
                return Collections.emptyList();
            }

            public java.util.List<MavenProject> getAllSortedProjects() {
                return Collections.emptyList();
            }
        });

        final List<String> log = new ArrayList<>();

        MojoExecutionListener mojoListener = new MojoExecutionListener() {
            public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNotNull(event.getExecution());
                assertNotNull(event.getMojo());
                assertNull(event.getCause());

                log.add("beforeMojoExecution " + event.getProject().getArtifactId() + ":"
                        + event.getExecution().getExecutionId());
            }

            public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNotNull(event.getExecution());
                assertNotNull(event.getMojo());
                assertNull(event.getCause());

                log.add("afterMojoExecutionSuccess " + event.getProject().getArtifactId() + ":"
                        + event.getExecution().getExecutionId());
            }

            public void afterExecutionFailure(MojoExecutionEvent event) {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNotNull(event.getExecution());
                assertNotNull(event.getMojo());
                assertNotNull(event.getCause());

                log.add("afterExecutionFailure " + event.getProject().getArtifactId() + ":"
                        + event.getExecution().getExecutionId());
            }
        };
        ProjectExecutionListener projectListener = new ProjectExecutionListener() {
            public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNull(event.getExecutionPlan());
                assertNull(event.getCause());

                log.add("beforeProjectExecution " + event.getProject().getArtifactId());
            }

            public void beforeProjectLifecycleExecution(ProjectExecutionEvent event)
                    throws LifecycleExecutionException {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNotNull(event.getExecutionPlan());
                assertNull(event.getCause());

                log.add("beforeProjectLifecycleExecution " + event.getProject().getArtifactId());
            }

            public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNotNull(event.getExecutionPlan());
                assertNull(event.getCause());

                log.add("afterProjectExecutionSuccess " + event.getProject().getArtifactId());
            }

            public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
                assertNotNull(event.getSession());
                assertNotNull(event.getProject());
                assertNull(event.getExecutionPlan());
                assertNotNull(event.getCause());

                log.add("afterProjectExecutionFailure " + event.getProject().getArtifactId());
            }
        };
        lookup(DelegatingProjectExecutionListener.class).addProjectExecutionListener(projectListener);
        lookup(DelegatingMojoExecutionListener.class).addMojoExecutionListener(mojoListener);

        try {
            lifecycleExecutor.execute(session);
        } finally {
            lookup(DelegatingProjectExecutionListener.class).removeProjectExecutionListener(projectListener);
            lookup(DelegatingMojoExecutionListener.class).removeMojoExecutionListener(mojoListener);
        }

        List<String> expectedLog = Arrays.asList(
                "beforeProjectExecution project-basic", //
                "beforeProjectLifecycleExecution project-basic", //
                "beforeMojoExecution project-basic:default-resources", //
                "afterMojoExecutionSuccess project-basic:default-resources", //
                "beforeMojoExecution project-basic:default-compile", //
                "afterMojoExecutionSuccess project-basic:default-compile", //
                "beforeMojoExecution project-basic:default-testResources", //
                "afterMojoExecutionSuccess project-basic:default-testResources", //
                "beforeMojoExecution project-basic:default-testCompile", //
                "afterMojoExecutionSuccess project-basic:default-testCompile", //
                "beforeMojoExecution project-basic:default-test", //
                "afterMojoExecutionSuccess project-basic:default-test", //
                "beforeMojoExecution project-basic:default-jar", //
                "afterMojoExecutionSuccess project-basic:default-jar", //
                "afterProjectExecutionSuccess project-basic" //
                );

        assertEventLog(expectedLog, log);
    }

    private static void assertEventLog(List<String> expectedList, List<String> actualList) {
        assertEquals(toString(expectedList), toString(actualList));
    }

    private static String toString(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
