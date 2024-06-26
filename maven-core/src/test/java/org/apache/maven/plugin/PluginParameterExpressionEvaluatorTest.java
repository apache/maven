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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author Jason van Zyl
 */
public class PluginParameterExpressionEvaluatorTest extends AbstractCoreMavenComponentTestCase {
    private static final String FS = File.separator;

    private RepositorySystem factory;

    public void setUp() throws Exception {
        super.setUp();
        factory = lookup(RepositorySystem.class);
    }

    @Override
    protected void tearDown() throws Exception {
        factory = null;
        super.tearDown();
    }

    public void testPluginDescriptorExpressionReference() throws Exception {
        MojoExecution exec = newMojoExecution();

        MavenSession session = newMavenSession();

        Object result = new PluginParameterExpressionEvaluator(session, exec).evaluate("${plugin}");

        System.out.println("Result: " + result);

        assertSame(
                "${plugin} expression does not return plugin descriptor.",
                exec.getMojoDescriptor().getPluginDescriptor(),
                result);
    }

    public void testPluginArtifactsExpressionReference() throws Exception {
        MojoExecution exec = newMojoExecution();

        Artifact depArtifact = createArtifact("group", "artifact", "1");

        List<Artifact> deps = new ArrayList<>();
        deps.add(depArtifact);

        exec.getMojoDescriptor().getPluginDescriptor().setArtifacts(deps);

        MavenSession session = newMavenSession();

        @SuppressWarnings("unchecked")
        List<Artifact> depResults =
                (List<Artifact>) new PluginParameterExpressionEvaluator(session, exec).evaluate("${plugin.artifacts}");

        System.out.println("Result: " + depResults);

        assertNotNull(depResults);
        assertEquals(1, depResults.size());
        assertSame("dependency artifact is wrong.", depArtifact, depResults.get(0));
    }

    public void testPluginArtifactMapExpressionReference() throws Exception {
        MojoExecution exec = newMojoExecution();

        Artifact depArtifact = createArtifact("group", "artifact", "1");

        List<Artifact> deps = new ArrayList<>();
        deps.add(depArtifact);

        exec.getMojoDescriptor().getPluginDescriptor().setArtifacts(deps);

        MavenSession session = newMavenSession();

        @SuppressWarnings("unchecked")
        Map<String, Artifact> depResults = (Map<String, Artifact>)
                new PluginParameterExpressionEvaluator(session, exec).evaluate("${plugin.artifactMap}");

        System.out.println("Result: " + depResults);

        assertNotNull(depResults);
        assertEquals(1, depResults.size());
        assertSame(
                "dependency artifact is wrong.",
                depArtifact,
                depResults.get(ArtifactUtils.versionlessKey(depArtifact)));
    }

    public void testPluginArtifactIdExpressionReference() throws Exception {
        MojoExecution exec = newMojoExecution();

        MavenSession session = newMavenSession();

        Object result = new PluginParameterExpressionEvaluator(session, exec).evaluate("${plugin.artifactId}");

        System.out.println("Result: " + result);

        assertSame(
                "${plugin.artifactId} expression does not return plugin descriptor's artifactId.",
                exec.getMojoDescriptor().getPluginDescriptor().getArtifactId(),
                result);
    }

    public void testValueExtractionWithAPomValueContainingAPath() throws Exception {
        String expected = getTestFile("target/test-classes/target/classes").getCanonicalPath();

        Build build = new Build();
        build.setDirectory(expected.substring(0, expected.length() - "/classes".length()));

        Model model = new Model();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setFile(new File("pom.xml").getCanonicalFile());

        ExpressionEvaluator expressionEvaluator = createExpressionEvaluator(project, null, new Properties());

        Object value = expressionEvaluator.evaluate("${project.build.directory}/classes");
        String actual = new File(value.toString()).getCanonicalPath();

        assertEquals(expected, actual);
    }

    public void testEscapedVariablePassthrough() throws Exception {
        String var = "${var}";

        Model model = new Model();
        model.setVersion("1");

        MavenProject project = new MavenProject(model);

        ExpressionEvaluator ee = createExpressionEvaluator(project, null, new Properties());

        Object value = ee.evaluate("$" + var);

        assertEquals(var, value);
    }

    public void testEscapedVariablePassthroughInLargerExpression() throws Exception {
        String var = "${var}";
        String key = var + " with version: ${project.version}";

        Model model = new Model();
        model.setVersion("1");

        MavenProject project = new MavenProject(model);

        ExpressionEvaluator ee = createExpressionEvaluator(project, null, new Properties());

        Object value = ee.evaluate("$" + key);

        assertEquals("${var} with version: 1", value);
    }

    public void testMultipleSubExpressionsInLargerExpression() throws Exception {
        String key = "${project.artifactId} with version: ${project.version}";

        Model model = new Model();
        model.setArtifactId("test");
        model.setVersion("1");

        MavenProject project = new MavenProject(model);

        ExpressionEvaluator ee = createExpressionEvaluator(project, null, new Properties());

        Object value = ee.evaluate(key);

        assertEquals("test with version: 1", value);
    }

    public void testMissingPOMPropertyRefInLargerExpression() throws Exception {
        String expr = "/path/to/someproject-${baseVersion}";

        MavenProject project = new MavenProject(new Model());

        ExpressionEvaluator ee = createExpressionEvaluator(project, null, new Properties());

        Object value = ee.evaluate(expr);

        assertEquals(expr, value);
    }

    public void testPOMPropertyExtractionWithMissingProject_WithDotNotation() throws Exception {
        String key = "m2.name";
        String checkValue = "value";

        Properties properties = new Properties();
        properties.setProperty(key, checkValue);

        Model model = new Model();
        model.setProperties(properties);

        MavenProject project = new MavenProject(model);

        ExpressionEvaluator ee = createExpressionEvaluator(project, null, new Properties());

        Object value = ee.evaluate("${" + key + "}");

        assertEquals(checkValue, value);
    }

    public void testBasedirExtractionWithMissingProject() throws Exception {
        ExpressionEvaluator ee = createExpressionEvaluator(null, null, new Properties());

        Object value = ee.evaluate("${basedir}");

        assertEquals(System.getProperty("user.dir"), value);
    }

    public void testValueExtractionFromSystemPropertiesWithMissingProject() throws Exception {
        String sysprop = "PPEET_sysprop1";

        Properties executionProperties = new Properties();

        if (executionProperties.getProperty(sysprop) == null) {
            executionProperties.setProperty(sysprop, "value");
        }

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("${" + sysprop + "}");

        assertEquals("value", value);
    }

    public void testValueExtractionOfMissingPrefixedProperty() throws Exception {
        Properties executionProperties = new Properties();

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("prefix-${PPEET_nonexisting_p_property}");

        assertEquals("prefix-${PPEET_nonexisting_p_property}", value);
    }

    public void testValueExtractionOfMissingSuffixedProperty() throws Exception {
        Properties executionProperties = new Properties();

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("${PPEET_nonexisting_s_property}-suffix");

        assertEquals("${PPEET_nonexisting_s_property}-suffix", value);
    }

    public void testValueExtractionOfMissingPrefixedSuffixedProperty() throws Exception {
        Properties executionProperties = new Properties();

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("prefix-${PPEET_nonexisting_ps_property}-suffix");

        assertEquals("prefix-${PPEET_nonexisting_ps_property}-suffix", value);
    }

    public void testValueExtractionOfMissingProperty() throws Exception {
        Properties executionProperties = new Properties();

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("${PPEET_nonexisting_property}");

        assertNull(value);
    }

    public void testValueExtractionFromSystemPropertiesWithMissingProject_WithDotNotation() throws Exception {
        String sysprop = "PPEET.sysprop2";

        Properties executionProperties = new Properties();

        if (executionProperties.getProperty(sysprop) == null) {
            executionProperties.setProperty(sysprop, "value");
        }

        ExpressionEvaluator ee = createExpressionEvaluator(null, null, executionProperties);

        Object value = ee.evaluate("${" + sysprop + "}");

        assertEquals("value", value);
    }

    @SuppressWarnings("deprecation")
    private static MavenSession createSession(PlexusContainer container, ArtifactRepository repo, Properties properties)
            throws CycleDetectedException, DuplicateProjectException {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setSystemProperties(properties)
                .setGoals(Collections.<String>emptyList())
                .setBaseDirectory(new File(""))
                .setLocalRepository(repo);

        return new MavenSession(
                container, request, new DefaultMavenExecutionResult(), Collections.<MavenProject>emptyList());
    }

    public void testLocalRepositoryExtraction() throws Exception {
        ExpressionEvaluator expressionEvaluator =
                createExpressionEvaluator(createDefaultProject(), null, new Properties());
        Object value = expressionEvaluator.evaluate("${localRepository}");

        assertEquals("local", ((ArtifactRepository) value).getId());
    }

    public void testTwoExpressions() throws Exception {
        Build build = new Build();
        build.setDirectory("expected-directory");
        build.setFinalName("expected-finalName");

        Model model = new Model();
        model.setBuild(build);

        ExpressionEvaluator expressionEvaluator =
                createExpressionEvaluator(new MavenProject(model), null, new Properties());

        Object value = expressionEvaluator.evaluate("${project.build.directory}" + FS + "${project.build.finalName}");

        assertEquals("expected-directory" + File.separatorChar + "expected-finalName", value);
    }

    public void testShouldExtractPluginArtifacts() throws Exception {
        PluginDescriptor pd = new PluginDescriptor();

        Artifact artifact = createArtifact("testGroup", "testArtifact", "1.0");

        pd.setArtifacts(Collections.singletonList(artifact));

        ExpressionEvaluator ee = createExpressionEvaluator(createDefaultProject(), pd, new Properties());

        Object value = ee.evaluate("${plugin.artifacts}");

        assertTrue(value instanceof List);

        @SuppressWarnings("unchecked")
        List<Artifact> artifacts = (List<Artifact>) value;

        assertEquals(1, artifacts.size());

        Artifact result = artifacts.get(0);

        assertEquals("testGroup", result.getGroupId());
    }

    private MavenProject createDefaultProject() {
        return new MavenProject(new Model());
    }

    private ExpressionEvaluator createExpressionEvaluator(
            MavenProject project, PluginDescriptor pluginDescriptor, Properties executionProperties) throws Exception {
        ArtifactRepository repo = factory.createDefaultLocalRepository();

        MutablePlexusContainer container = (MutablePlexusContainer) getContainer();
        MavenSession session = createSession(container, repo, executionProperties);
        session.setCurrentProject(project);

        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setPluginDescriptor(pluginDescriptor);
        mojo.setGoal("goal");

        MojoExecution mojoExecution = new MojoExecution(mojo);

        return new PluginParameterExpressionEvaluator(session, mojoExecution);
    }

    protected Artifact createArtifact(String groupId, String artifactId, String version) throws Exception {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType("jar");
        dependency.setScope("compile");

        return factory.createDependencyArtifact(dependency);
    }

    private MojoExecution newMojoExecution() {
        PluginDescriptor pd = new PluginDescriptor();
        pd.setArtifactId("my-plugin");
        pd.setGroupId("org.myco.plugins");
        pd.setVersion("1");

        MojoDescriptor md = new MojoDescriptor();
        md.setPluginDescriptor(pd);

        pd.addComponentDescriptor(md);

        return new MojoExecution(md);
    }

    private MavenSession newMavenSession() throws Exception {
        return createMavenSession(null);
    }

    @Override
    protected String getProjectsDirectory() {
        // TODO Auto-generated method stub
        return null;
    }
}
