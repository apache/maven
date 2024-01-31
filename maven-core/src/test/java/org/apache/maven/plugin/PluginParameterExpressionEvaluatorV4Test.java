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
package org.apache.maven.plugin;

import javax.inject.Inject;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Session;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.configuration.internal.EnhancedComponentConfigurator;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.*;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.interpolation.reflection.IntrospectionException;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.CycleDetectedException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.util.Os;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 */
public class PluginParameterExpressionEvaluatorV4Test extends AbstractCoreMavenComponentTestCase {
    private static final String FS = File.separator;

    @Inject
    PlexusContainer container;

    private Path rootDirectory;

    @Test
    public void testPluginDescriptorExpressionReference() throws Exception {
        Session session = newSession();
        MojoExecution exec = newMojoExecution(session);

        Object result =
                new PluginParameterExpressionEvaluatorV4(session, null, exec).evaluate("${mojo.plugin.descriptor}");

        System.out.println("Result: " + result);

        assertSame(
                exec.getPlugin().getDescriptor(),
                result,
                "${mojo.plugin.descriptor} expression does not return plugin descriptor.");
    }

    @Test
    public void testPluginArtifactsExpressionReference() throws Exception {
        Session session = newSession();
        MojoExecution exec = newMojoExecution(session);

        @SuppressWarnings("unchecked")
        Collection<Artifact> depResults = (Collection<Artifact>)
                new PluginParameterExpressionEvaluatorV4(session, null, exec).evaluate("${mojo.plugin.dependencies}");

        System.out.println("Result: " + depResults);

        assertNotNull(depResults);
        assertEquals(1, depResults.size());
        assertEquals(
                exec.getPlugin().getArtifact().key(),
                depResults.iterator().next().key(),
                "dependency artifact is wrong.");
    }

    @Test
    public void testPluginArtifactMapExpressionReference() throws Exception {
        Session session = newSession();

        MojoExecution exec = newMojoExecution(session);

        @SuppressWarnings("unchecked")
        Map<String, org.apache.maven.api.Dependency> depResults = (Map<String, org.apache.maven.api.Dependency>)
                new PluginParameterExpressionEvaluatorV4(session, null, exec)
                        .evaluate("${mojo.plugin.dependenciesMap}");

        System.out.println("Result: " + depResults);

        assertNotNull(depResults);
        assertEquals(1, depResults.size());
        assertTrue(depResults.containsKey("org.myco.plugins:my-plugin"));
        assertEquals(
                exec.getPlugin().getArtifact().key(),
                depResults.get("org.myco.plugins:my-plugin").key(),
                "dependency artifact is wrong.");
    }

    @Test
    public void testPluginArtifactIdExpressionReference() throws Exception {
        Session session = newSession();

        MojoExecution exec = newMojoExecution(session);

        Object result = new PluginParameterExpressionEvaluatorV4(session, null, exec)
                .evaluate("${mojo.plugin.artifact.artifactId}");

        System.out.println("Result: " + result);

        assertSame(
                exec.getPlugin().getArtifact().getArtifactId(),
                result,
                "${plugin.artifactId} expression does not return plugin descriptor's artifactId.");
    }

    @Test
    public void testValueExtractionWithAPomValueContainingAPath() throws Exception {
        String expected = getTestFile("target/test-classes/target/classes").getCanonicalPath();

        Build build = new Build();
        build.setDirectory(expected.substring(0, expected.length() - "/classes".length()));

        Model model = new Model();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setFile(new File("pom.xml").getCanonicalFile());

        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator(project, new Properties());

        Object value = expressionEvaluator.evaluate("${project.build.directory}/classes");
        String actual = new File(value.toString()).getCanonicalPath();

        assertEquals(expected, actual);
    }

    @Test
    public void testEscapedVariablePassthrough() throws Exception {
        String var = "${var}";

        MavenProject project = createDefaultProject();
        project.setVersion("1");

        ExpressionEvaluator ee = createExpressionEvaluator(project, new Properties());

        Object value = ee.evaluate("$" + var);

        assertEquals(var, value);
    }

    @Test
    public void testEscapedVariablePassthroughInLargerExpression() throws Exception {
        String var = "${var}";
        String key = var + " with version: ${project.version}";

        MavenProject project = createDefaultProject();
        project.setVersion("1");

        ExpressionEvaluator ee = createExpressionEvaluator(project, new Properties());

        Object value = ee.evaluate("$" + key);

        assertEquals("${var} with version: 1", value);
    }

    @Test
    public void testMultipleSubExpressionsInLargerExpression() throws Exception {
        String key = "${project.artifactId} with version: ${project.version}";

        MavenProject project = createDefaultProject();
        project.setArtifactId("test");
        project.setVersion("1");

        ExpressionEvaluator ee = createExpressionEvaluator(project, new Properties());

        Object value = ee.evaluate(key);

        assertEquals("test with version: 1", value);
    }

    @Test
    public void testMissingPOMPropertyRefInLargerExpression() throws Exception {
        String expr = "/path/to/someproject-${baseVersion}";

        MavenProject project = createDefaultProject();

        ExpressionEvaluator ee = createExpressionEvaluator(project, new Properties());

        Object value = ee.evaluate(expr);

        assertEquals(expr, value);
    }

    @Test
    public void testPOMPropertyExtractionWithMissingProject_WithDotNotation() throws Exception {
        String key = "m2.name";
        String checkValue = "value";

        MavenProject project = createDefaultProject();
        project.getModel().getProperties().setProperty(key, checkValue);

        ExpressionEvaluator ee = createExpressionEvaluator(project, new Properties());

        Object value = ee.evaluate("${" + key + "}");

        assertEquals(checkValue, value);
    }

    @Test
    public void testValueExtractionFromSystemPropertiesWithMissingProject() throws Exception {
        String sysprop = "PPEET_sysprop1";

        Properties executionProperties = new Properties();

        if (executionProperties.getProperty(sysprop) == null) {
            executionProperties.setProperty(sysprop, "value");
        }

        ExpressionEvaluator ee = createExpressionEvaluator(null, executionProperties);

        Object value = ee.evaluate("${" + sysprop + "}");

        assertEquals("value", value);
    }

    @Test
    public void testValueExtractionFromSystemPropertiesWithMissingProject_WithDotNotation() throws Exception {
        String sysprop = "PPEET.sysprop2";

        Properties executionProperties = new Properties();

        if (executionProperties.getProperty(sysprop) == null) {
            executionProperties.setProperty(sysprop, "value");
        }

        ExpressionEvaluator ee = createExpressionEvaluator(null, executionProperties);

        Object value = ee.evaluate("${" + sysprop + "}");

        assertEquals("value", value);
    }

    @SuppressWarnings("deprecation")
    private static MavenSession createSession(PlexusContainer container, ArtifactRepository repo, Properties properties)
            throws CycleDetectedException, DuplicateProjectException, NoLocalRepositoryManagerException {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setSystemProperties(properties)
                .setGoals(Collections.<String>emptyList())
                .setBaseDirectory(new File(""))
                .setLocalRepository(repo);

        DefaultRepositorySystemSession repositorySession =
                new DefaultRepositorySystemSession(h -> false); // no close handle
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(repo.getUrl())));
        MavenSession session =
                new MavenSession(container, repositorySession, request, new DefaultMavenExecutionResult());
        session.setProjects(Collections.<MavenProject>emptyList());
        return session;
    }

    @Test
    public void testTwoExpressions() throws Exception {
        MavenProject project = createDefaultProject();
        Build build = project.getBuild();
        build.setDirectory("expected-directory");
        build.setFinalName("expected-finalName");

        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator(project, new Properties());

        Object value = expressionEvaluator.evaluate("${project.build.directory}" + FS + "${project.build.finalName}");

        assertEquals("expected-directory" + File.separatorChar + "expected-finalName", value);
    }

    @Test
    public void testShouldExtractPluginArtifacts() throws Exception {
        ExpressionEvaluator ee = createExpressionEvaluator(createDefaultProject(), new Properties());

        Object value = ee.evaluate("${mojo.plugin.dependencies}");

        assertTrue(value instanceof Collection);

        @SuppressWarnings("unchecked")
        Collection<Artifact> artifacts = (Collection<Artifact>) value;

        assertEquals(1, artifacts.size());

        Artifact result = artifacts.iterator().next();

        assertEquals("org.myco.plugins", result.getGroupId());
    }

    @Test
    void testRootDirectoryNotPrefixed() throws Exception {
        ExpressionEvaluator ee = createExpressionEvaluator(createDefaultProject(), new Properties());
        assertNull(ee.evaluate("${rootDirectory}"));
    }

    @Test
    void testRootDirectoryWithNull() throws Exception {
        ExpressionEvaluator ee = createExpressionEvaluator(createDefaultProject(), new Properties());
        Exception e = assertThrows(Exception.class, () -> ee.evaluate("${session.rootDirectory}"));
        e = assertInstanceOf(IntrospectionException.class, e.getCause());
        e = assertInstanceOf(IllegalStateException.class, e.getCause());
        assertEquals(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE, e.getMessage());
    }

    @Test
    void testRootDirectory() throws Exception {
        this.rootDirectory = Paths.get("myRootDirectory");
        ExpressionEvaluator ee = createExpressionEvaluator(createDefaultProject(), new Properties());
        assertInstanceOf(Path.class, ee.evaluate("${session.rootDirectory}"));
    }

    private MavenProject createDefaultProject() {
        MavenProject project = new MavenProject(new Model());
        project.setFile(new File("pom.xml").getAbsoluteFile());
        return project;
    }

    private ExpressionEvaluator createExpressionEvaluator(MavenProject project, Properties executionProperties)
            throws Exception {
        ArtifactRepository repo = getLocalRepository();

        MutablePlexusContainer container = (MutablePlexusContainer) getContainer();
        MavenSession mavenSession = createSession(container, repo, executionProperties);
        mavenSession.setCurrentProject(project);
        mavenSession.getRequest().setRootDirectory(rootDirectory);
        mavenSession.getRequest().setTopDirectory(rootDirectory);

        DefaultSession session = new DefaultSession(
                mavenSession, mock(RepositorySystem.class), null, null, new DefaultLookup(container), null);

        MojoExecution mojoExecution = newMojoExecution(session);

        return new PluginParameterExpressionEvaluatorV4(
                session, project != null ? new DefaultProject(session, project) : null, mojoExecution);
    }

    private MojoExecution newMojoExecution(Session session) {
        PluginDescriptor pd = new PluginDescriptor();
        pd.setArtifactId("my-plugin");
        pd.setGroupId("org.myco.plugins");
        pd.setVersion("1");

        DefaultArtifact artifact = new DefaultArtifact(
                pd.getGroupId(),
                pd.getArtifactId(),
                pd.getVersion(),
                "compile",
                "maven-plugin",
                "",
                new DefaultArtifactHandler("maven-plugin"));
        pd.setPluginArtifact(artifact);

        pd.setArtifacts(Collections.singletonList(artifact));
        DefaultDependencyNode node = new DefaultDependencyNode(
                new org.eclipse.aether.graph.Dependency(RepositoryUtils.toArtifact(artifact), "compile"));
        pd.setDependencyNode(node);

        MojoDescriptor md = new MojoDescriptor();
        md.setGoal("my-goal");
        md.setPluginDescriptor(pd);

        pd.addComponentDescriptor(md);

        return new DefaultMojoExecution((AbstractSession) session, new org.apache.maven.plugin.MojoExecution(md));
    }

    private DefaultSession newSession() throws Exception {
        DefaultSession session = new DefaultSession(
                newMavenSession(), mock(RepositorySystem.class), null, null, new DefaultLookup(container), null);
        return session;
    }

    private MavenSession newMavenSession() throws Exception {
        return createMavenSession(null);
    }

    @Test
    public void testUri() throws Exception {
        Path path = Paths.get("").toAbsolutePath();

        MavenSession mavenSession = createMavenSession(null);
        mavenSession.getRequest().setTopDirectory(path);
        mavenSession.getRequest().setRootDirectory(path);

        Object result = new PluginParameterExpressionEvaluatorV4(mavenSession.getSession(), null)
                .evaluate("${session.rootDirectory.uri}");
        assertEquals(path.toUri(), result);
    }

    @Test
    public void testPath() throws Exception {
        Path path = Paths.get("").toAbsolutePath();

        MavenSession mavenSession = createMavenSession(null);
        mavenSession.getRequest().setTopDirectory(path);
        mavenSession.getRequest().setRootDirectory(path);

        Object result = new PluginParameterExpressionEvaluatorV4(mavenSession.getSession(), null)
                .evaluate("${session.rootDirectory/target}");
        assertEquals(path.resolve("target"), result);
    }

    @Test
    public void testPluginInjection() throws Exception {
        Path path = Paths.get("rép➜α").toAbsolutePath();

        MavenSession mavenSession = createMavenSession(null);
        mavenSession.getRequest().setTopDirectory(path);
        mavenSession.getRequest().setRootDirectory(path);
        DefaultModelBuildingRequest mbr = new DefaultModelBuildingRequest();

        PluginParameterExpressionEvaluatorV4 evaluator =
                new PluginParameterExpressionEvaluatorV4(mavenSession.getSession(), null);

        DefaultPlexusConfiguration configuration = new DefaultPlexusConfiguration("config");
        configuration.addChild("uri", "${session.rootDirectory.uri}");
        configuration.addChild("path", "${session.rootDirectory}");
        configuration.addChild("uriString", "${session.rootDirectory.uri.string}");
        configuration.addChild("uriAsciiString", "${session.rootDirectory.uri.ASCIIString}");
        configuration.addChild("pathString", "${session.rootDirectory.string}");

        Mojo mojo = new Mojo();
        new EnhancedComponentConfigurator().configureComponent(mojo, configuration, evaluator, null);

        assertEquals(
                Objects.equals(path.toUri().toString(), path.toUri().toASCIIString()), !Os.isFamily(Os.FAMILY_WINDOWS));
        assertEquals(mojo.uri, path.toUri());
        assertEquals(mojo.path, path);
        assertEquals(mojo.uriString, path.toUri().toString());
        assertEquals(mojo.uriAsciiString, path.toUri().toASCIIString());
        assertEquals(mojo.pathString, path.toString());
    }

    @Override
    protected String getProjectsDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    public static class Mojo {
        URI uri;
        Path path;
        String uriString;
        String uriAsciiString;
        String pathString;
    }
}
