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
package org.apache.maven.configuration.internal;

import javax.inject.Inject;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.plugin.descriptor.Parameter;
import org.apache.maven.api.services.MojoConfigurationService;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.impl.DefaultLookup;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.DefaultProject;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultMojoConfigurationServiceTest extends AbstractCoreMavenComponentTestCase {

    @Inject
    PlexusContainer container;

    org.apache.maven.execution.MavenSession mavenSession;

    DefaultSession session;

    @BeforeEach
    void setUp() throws Exception {
        mavenSession = createMavenSession(null);
        session = new DefaultSession(
                mavenSession, mock(RepositorySystem.class), null, null, new DefaultLookup(container), null);
    }

    @Test
    void readsConfiguredParameterWithoutInstantiatingMojo() throws Exception {
        TestMojo.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("message")
                .alias("display-message")
                .type(String.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-plugin-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom message = new Xpp3Dom("display-message");
        message.setValue("Hello Maven");
        configuration.addChild(message);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);

        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        MojoConfigurationService service = session.getService(MojoConfigurationService.class);
        assertNotNull(service);

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals("Hello Maven", parameters.get("message"));
        assertEquals(0, TestMojo.setterCalls, "Mojo setter must not be invoked");
    }

    @Test
    void resolvesParametersUsingLegacyV3ExpressionEvaluator() throws Exception {
        TestMojo.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-v3-plugin-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor();
        legacyDescriptor.setPluginDescriptor(pluginDescriptor);
        legacyDescriptor.setGoal("test");
        legacyDescriptor.setImplementation(TestMojo.class.getName());

        org.apache.maven.plugin.descriptor.Parameter legacyParameter =
                new org.apache.maven.plugin.descriptor.Parameter();
        legacyParameter.setName("message");
        legacyParameter.setType(String.class.getName());
        legacyDescriptor.addParameter(legacyParameter);

        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom message = new Xpp3Dom("message");
        message.setValue("${project.artifactId}");
        configuration.addChild(message);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);

        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        MojoConfigurationService service = session.getService(MojoConfigurationService.class);
        assertNotNull(service);

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertFalse(legacyDescriptor.isV4Api());
        assertEquals(mavenProject.getArtifactId(), parameters.get("message"));
        assertEquals(0, TestMojo.setterCalls, "Mojo setter must not be invoked");
    }

    @Test
    void resolvesProjectBasedirExpression() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("outputDirectory")
                .type(Path.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-expression-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom outputDirectory = new Xpp3Dom("outputDirectory");
        outputDirectory.setValue("${project.basedir}");
        configuration.addChild(outputDirectory);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals(project.getBasedir(), parameters.get("outputDirectory"));
    }

    @Test
    void convertsConfiguredIntegerParameter() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("count")
                .type(Integer.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-integer-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom count = new Xpp3Dom("count");
        count.setValue("42");
        configuration.addChild(count);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals(42, parameters.get("count"));
        assertInstanceOf(Integer.class, parameters.get("count"));
    }

    @Test
    void convertsConfiguredBooleanParameter() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("enabled")
                .type(Boolean.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-boolean-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom enabled = new Xpp3Dom("enabled");
        enabled.setValue("true");
        configuration.addChild(enabled);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals(true, parameters.get("enabled"));
        assertInstanceOf(Boolean.class, parameters.get("enabled"));
    }

    @Test
    void resolvesRuntimeProjectPropertyExpression() throws Exception {
        TestMojo.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));

        mavenProject.getProperties().setProperty("runtime.customValue", "runtime-value");

        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("message")
                .type(String.class.getName())
                .build();

        Parameter escaped = Parameter.newBuilder()
                .name("setterOnly")
                .type(String.class.getName())
                .build();

        Parameter unresolved = Parameter.newBuilder()
                .name("defaultMessage")
                .type(String.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter, escaped, unresolved))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm =
                world.newRealm("test-runtime-property-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(new Xpp3Dom("message"));
        configuration.getChild("message").setValue("${runtime.customValue}");
        configuration.addChild(new Xpp3Dom("setterOnly"));
        configuration.getChild("setterOnly").setValue("$${runtime.customValue}");
        configuration.addChild(new Xpp3Dom("defaultMessage"));
        configuration.getChild("defaultMessage").setValue("${runtime.unresolved}");

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals("runtime-value", parameters.get("message"));
        assertEquals("runtime-value", parameters.get("setterOnly"));
        assertFalse(parameters.containsKey("defaultMessage"));
        assertEquals(0, TestMojo.setterCalls, "Mojo setter must not be invoked");
    }

    @Test
    void resolvesRuntimeProjectPropertyInUriLikeMavenConfiguration() throws Exception {
        TestMojo.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        mavenProject.getProperties().setProperty("runtime.uri", "https://example.invalid/runtime-path");
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter =
                Parameter.newBuilder().name("uri").type(URI.class.getName()).build();
        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");
        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-runtime-uri-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom uri = new Xpp3Dom("uri");
        uri.setValue("${runtime.uri}");
        configuration.addChild(uri);
        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));
        Map<String, Object> parameters = service.getParameters(project, execution);

        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluatorV4(session, project, execution);
        org.apache.maven.execution.MavenSession interpolationSession = mavenSession.clone();
        interpolationSession.setCurrentProject(mavenProject);
        PlexusConfiguration normalConfiguration = XmlPlexusConfiguration.toPlexusConfiguration(
                execution.getConfiguration().orElseThrow());
        normalConfiguration = PluginConfigurationInterpolator.interpolateConfiguration(
                normalConfiguration, evaluator, interpolationSession);
        TestUriBean bean = new TestUriBean();
        new EnhancedComponentConfigurator().configureComponent(bean, normalConfiguration, evaluator, realm, null);

        assertEquals(URI.create("https://example.invalid/runtime-path"), parameters.get("uri"));
        assertEquals(bean.getUri(), parameters.get("uri"));
        assertEquals(0, TestMojo.setterCalls, "Mojo setter must not be invoked");
    }

    @Test
    void readsSetterOnlyParameterWithoutInvokingSetter() throws Exception {
        TestMojo.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("setterOnly")
                .type(String.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-setter-only-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom setterOnly = new Xpp3Dom("setterOnly");
        setterOnly.setValue("setter-only-value");
        configuration.addChild(setterOnly);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals("setter-only-value", parameters.get("setterOnly"));
        assertEquals(0, TestMojo.setterCalls, "Mojo setter must not be invoked");
    }

    @Test
    void resolvesDefaultParameterValue() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("defaultMessage")
                .type(String.class.getName())
                .defaultValue("default-value")
                .build();

        Parameter retries = Parameter.newBuilder()
                .name("retries")
                .type(Integer.class.getName())
                .defaultValue("5")
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter, retries))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-default-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, new Xpp3Dom("configuration"));
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals("default-value", parameters.get("defaultMessage"));
        assertEquals(5, parameters.get("retries"));
        assertInstanceOf(Integer.class, parameters.get("retries"));
    }

    @Test
    void returnsXmlForPluginSpecificDefaultParameter() throws Exception {
        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("customOptions")
                .type(TestOptions.class.getName())
                .defaultValue("custom-default")
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm =
                world.newRealm("test-custom-default-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, new Xpp3Dom("configuration"));
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        XmlNode resolvedValue = (XmlNode) parameters.get("customOptions");

        assertNotNull(resolvedValue);
        assertEquals("customOptions", resolvedValue.name());
        assertEquals("custom-default", resolvedValue.value());
    }

    @Test
    void convertsConfiguredStringListParameter() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("compilerArgs")
                .type(List.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-list-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom compilerArgs = new Xpp3Dom("compilerArgs");
        compilerArgs.addChild(new Xpp3Dom("arg"));
        compilerArgs.getChild(0).setValue("-Xlint");
        Xpp3Dom secondArg = new Xpp3Dom("arg");
        secondArg.setValue("-parameters");
        compilerArgs.addChild(secondArg);
        configuration.addChild(compilerArgs);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        assertEquals(List.of("-Xlint", "-parameters"), parameters.get("compilerArgs"));
    }

    @Test
    void returnsNestedPluginSpecificConfigurationAsXmlWithoutInvokingSetters() throws Exception {
        TestOptions.setterCalls = 0;

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        // Create nested configuration
        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom optionsConfig = new Xpp3Dom("options");

        Xpp3Dom compiler = new Xpp3Dom("compiler");
        compiler.setValue("javac");
        optionsConfig.addChild(compiler);

        Xpp3Dom source = new Xpp3Dom("source");
        source.setValue("17");
        optionsConfig.addChild(source);

        configuration.addChild(optionsConfig);

        // Define the Mojo parameter
        Parameter parameter = Parameter.newBuilder()
                .name("options")
                .type(TestOptions.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        // Create plugin descriptor and realm
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-nested-realm", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(realm);

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        // Create Mojo execution
        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, configuration);

        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        // Resolve parameters
        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), mock(MavenPluginManager.class));

        Map<String, Object> parameters = service.getParameters(project, execution);

        // Verify nested XML configuration
        XmlNode resolvedOptions = (XmlNode) parameters.get("options");

        assertNotNull(resolvedOptions);
        assertEquals("options", resolvedOptions.name());
        assertEquals("javac", resolvedOptions.child("compiler").value());
        assertEquals("17", resolvedOptions.child("source").value());
        assertEquals(0, TestOptions.setterCalls, "Nested parameter setters must not run");
    }

    @Test
    void createsPluginRealmUsingSuppliedProjectContext() throws Exception {

        org.apache.maven.project.MavenProject mavenProject =
                mavenSession.getProjects().get(0);
        org.apache.maven.project.MavenProject originalCurrentProject = mavenSession.getCurrentProject();

        mavenProject.setFile(new File(System.getProperty("user.dir"), "pom.xml"));
        Project project = new DefaultProject(InternalMavenSession.from(session), mavenProject);

        Parameter parameter = Parameter.newBuilder()
                .name("message")
                .type(String.class.getName())
                .build();

        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .parameters(List.of(parameter))
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);
        pluginDescriptor.addComponentDescriptor(legacyDescriptor);

        org.apache.maven.plugin.MojoExecution delegate =
                new org.apache.maven.plugin.MojoExecution(legacyDescriptor, new Xpp3Dom("configuration"));
        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        ClassRealm realm = new ClassRealm(new ClassWorld(), "test", getClass().getClassLoader());
        MavenPluginManager pluginManager = mock(MavenPluginManager.class);

        doAnswer(invocation -> {
                    org.apache.maven.execution.MavenSession realmSession = invocation.getArgument(1);
                    assertEquals(mavenProject, realmSession.getCurrentProject());
                    pluginDescriptor.setClassRealm(realm);
                    return null;
                })
                .when(pluginManager)
                .setupPluginRealm(eq(pluginDescriptor), any(), isNull(), isNull(), isNull());

        DefaultMojoConfigurationService service =
                new DefaultMojoConfigurationService(InternalMavenSession.from(session), pluginManager);

        service.getParameters(project, execution);

        assertEquals(originalCurrentProject, mavenSession.getCurrentProject());
        verify(pluginManager).setupPluginRealm(eq(pluginDescriptor), any(), isNull(), isNull(), isNull());
    }

    @Test
    void rejectsUnsupportedMojoExecutionImplementation() {
        MojoExecution mojoExecution = mock(MojoExecution.class);
        Project project = mock(Project.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultMojoConfigurationService(session, mock(MavenPluginManager.class))
                        .getParameters(project, mojoExecution));

        assertTrue(exception.getMessage().contains(mojoExecution.getClass().getName()));
    }

    @Test
    void rejectsUnsupportedProjectImplementation() throws Exception {
        org.apache.maven.api.plugin.descriptor.MojoDescriptor apiDescriptor =
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .implementation(TestMojo.class.getName())
                        .build();

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setVersion("1");

        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor =
                new org.apache.maven.plugin.descriptor.MojoDescriptor(pluginDescriptor, apiDescriptor);

        org.apache.maven.plugin.MojoExecution delegate = new org.apache.maven.plugin.MojoExecution(legacyDescriptor);

        MojoExecution execution = new DefaultMojoExecution(InternalMavenSession.from(session), delegate);

        Project unsupportedProject = mock(Project.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultMojoConfigurationService(
                                InternalMavenSession.from(session), mock(MavenPluginManager.class))
                        .getParameters(unsupportedProject, execution));

        assertTrue(exception.getMessage().contains(unsupportedProject.getClass().getName()));
    }

    @Override
    protected String getProjectsDirectory() {
        return "";
    }

    public static class TestOptions {
        static int setterCalls;

        private String compiler;
        private String source;

        public void setCompiler(String compiler) {
            setterCalls++;
            this.compiler = compiler;
        }

        public void setSource(String source) {
            setterCalls++;
            this.source = source;
        }
    }

    public static class TestUriBean {
        private URI uri;

        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }
    }

    static class TestMojo {
        static int setterCalls;

        private Path outputDirectory;
        private URI uri;
        private Integer count;
        private Boolean enabled;
        private Integer retries;
        private String setterOnly;
        private String defaultMessage;
        private TestOptions options;
        private List<String> compilerArgs;
        private TestOptions customOptions;

        TestMojo() {
            throw new AssertionError("Mojo must not be instantiated");
        }

        public void setMessage(String message) {
            setterCalls++;
        }

        public void setSetterOnly(String setterOnly) {
            setterCalls++;
        }
    }
}
