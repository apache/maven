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
package org.apache.maven.api.plugin.testing;

import javax.xml.stream.XMLStreamException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.testing.MavenDIExtension;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.ConfigurationContainer;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Source;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.plugin.descriptor.Parameter;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.testing.stubs.MojoExecutionStub;
import org.apache.maven.api.plugin.testing.stubs.PluginStub;
import org.apache.maven.api.plugin.testing.stubs.ProducedArtifactStub;
import org.apache.maven.api.plugin.testing.stubs.ProjectStub;
import org.apache.maven.api.plugin.testing.stubs.RepositorySystemSupplier;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.apache.maven.configuration.internal.EnhancedComponentConfigurator;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.model.DefaultModelPathTranslator;
import org.apache.maven.impl.model.DefaultPathTranslator;
import org.apache.maven.internal.impl.DefaultLog;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.v4.MavenMerger;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
import org.apache.maven.plugin.descriptor.io.PluginDescriptorStaxReader;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.aether.RepositorySystem;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * JUnit Jupiter extension that provides support for testing Maven plugins (Mojos).
 * This extension handles the lifecycle of Mojo instances in tests, including instantiation,
 * configuration, and dependency injection.
 *
 * <p>The extension is automatically registered when using the {@link MojoTest} annotation
 * on a test class. It provides the following features:</p>
 * <ul>
 *   <li>Automatic Mojo instantiation based on {@link InjectMojo} annotations</li>
 *   <li>Parameter injection using {@link MojoParameter} annotations</li>
 *   <li>POM configuration handling</li>
 *   <li>Project stub creation and configuration</li>
 *   <li>Maven session and build context setup</li>
 *   <li>Component dependency injection</li>
 * </ul>
 *
 * <p>Example usage in a test class:</p>
 * <pre>
 * {@code
 * @MojoTest
 * class MyMojoTest {
 *     @Test
 *     @InjectMojo(goal = "my-goal")
 *     @MojoParameter(name = "outputDirectory", value = "${project.build.directory}/generated")
 *     void testMojoExecution(MyMojo mojo) throws Exception {
 *         mojo.execute();
 *         // verify execution results
 *     }
 * }
 * }
 * </pre>
 *
 * <p>The extension supports two main injection scenarios:</p>
 * <ol>
 *   <li>Method parameter injection: Mojo instances can be injected as test method parameters</li>
 *   <li>Field injection: Components can be injected into test class fields using {@code @Inject}</li>
 * </ol>
 *
 * <p>For custom POM configurations, you can specify a POM file using the {@link InjectMojo#pom()}
 * attribute. The extension will merge this configuration with default test project settings.</p>
 *
 * <p>Base directory handling:</p>
 * <ul>
 *   <li>Plugin basedir: The directory containing the plugin project</li>
 *   <li>Test basedir: The directory containing test resources, configurable via {@link Basedir}</li>
 * </ul>
 *
 * @see MojoTest
 * @see InjectMojo
 * @see MojoParameter
 * @see Basedir
 * @since 4.0.0
 */
public class MojoExtension extends MavenDIExtension implements ParameterResolver, BeforeEachCallback {

    /** The base directory of the plugin being tested */
    protected static String pluginBasedir;

    /** The base directory for test resources */
    protected static String basedir;

    /**
     * Gets the identifier for the current test method.
     * The format is "TestClassName-testMethodName".
     *
     * @return the test identifier
     */
    public static String getTestId() {
        return context.getRequiredTestClass().getSimpleName() + "-"
                + context.getRequiredTestMethod().getName();
    }

    /**
     * Gets the base directory for test resources.
     * If not explicitly set via {@link Basedir}, returns the plugin base directory.
     *
     * @return the base directory path
     * @throws NullPointerException if neither basedir nor plugin basedir is set
     */
    public static String getBasedir() {
        return requireNonNull(basedir != null ? basedir : MavenDIExtension.basedir);
    }

    /**
     * Gets the base directory of the plugin being tested.
     *
     * @return the plugin base directory path
     * @throws NullPointerException if plugin basedir is not set
     */
    public static String getPluginBasedir() {
        return requireNonNull(pluginBasedir);
    }

    /**
     * Determines if this extension can resolve the given parameter.
     * Returns true if the parameter is annotated with {@link InjectMojo} or
     * if its declaring method is annotated with {@link InjectMojo}.
     *
     * @param parameterContext the context for the parameter being resolved
     * @param extensionContext the current extension context
     * @return true if this extension can resolve the parameter
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.isAnnotated(InjectMojo.class)
                || parameterContext.getDeclaringExecutable().isAnnotationPresent(InjectMojo.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        try {
            Class<?> holder = parameterContext.getTarget().orElseThrow().getClass();
            PluginDescriptor descriptor = extensionContext
                    .getStore(ExtensionContext.Namespace.GLOBAL)
                    .get(PluginDescriptor.class, PluginDescriptor.class);
            Model model =
                    extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).get(Model.class, Model.class);
            InjectMojo parameterInjectMojo =
                    parameterContext.getAnnotatedElement().getAnnotation(InjectMojo.class);
            String goal;
            if (parameterInjectMojo != null) {
                String pom = parameterInjectMojo.pom();
                if (pom != null && !pom.isEmpty()) {
                    try (Reader r = openPomUrl(holder, pom, new Path[1])) {
                        Model localModel = new MavenStaxReader().read(r);
                        model = new MavenMerger().merge(localModel, model, false, null);
                        model = new DefaultModelPathTranslator(new DefaultPathTranslator())
                                .alignToBaseDirectory(model, Paths.get(getBasedir()), null);
                    }
                }
                goal = parameterInjectMojo.goal();
            } else {
                InjectMojo methodInjectMojo = AnnotationSupport.findAnnotation(
                                parameterContext.getDeclaringExecutable(), InjectMojo.class)
                        .orElse(null);
                if (methodInjectMojo != null) {
                    goal = methodInjectMojo.goal();
                } else {
                    goal = getGoalFromMojoImplementationClass(
                            parameterContext.getParameter().getType());
                }
            }

            Set<MojoParameter> mojoParameters = new LinkedHashSet<>();
            for (AnnotatedElement ae :
                    Arrays.asList(parameterContext.getDeclaringExecutable(), parameterContext.getAnnotatedElement())) {
                mojoParameters.addAll(AnnotationSupport.findRepeatableAnnotations(ae, MojoParameter.class));
            }
            String[] coord = mojoCoordinates(goal);

            XmlNode pluginConfiguration = model.getBuild().getPlugins().stream()
                    .filter(p ->
                            Objects.equals(p.getGroupId(), coord[0]) && Objects.equals(p.getArtifactId(), coord[1]))
                    .findFirst()
                    .map(ConfigurationContainer::getConfiguration)
                    .orElseGet(() -> XmlNode.newInstance("config"));
            List<XmlNode> children = mojoParameters.stream()
                    .map(mp -> {
                        String value = mp.value();
                        if (!mp.xml()) {
                            // Treat as plain text - escape XML special characters
                            value = value.replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\"", "&quot;")
                                    .replace("'", "&apos;");
                        }
                        String s = '<' + mp.name() + '>' + value + "</" + mp.name() + '>';
                        try {
                            return XmlService.read(new StringReader(s));
                        } catch (XMLStreamException e) {
                            throw new MavenException("Unable to parse xml: " + e + System.lineSeparator() + s, e);
                        }
                    })
                    .collect(Collectors.toList());
            XmlNode config = XmlNode.newInstance("configuration", null, null, children, null);
            pluginConfiguration = XmlService.merge(config, pluginConfiguration);

            // load default config
            // pluginkey = groupId : artifactId : version : goal
            Mojo mojo = lookup(Mojo.class, coord[0] + ":" + coord[1] + ":" + coord[2] + ":" + coord[3]);
            for (MojoDescriptor mojoDescriptor : descriptor.getMojos()) {
                if (Objects.equals(mojoDescriptor.getGoal(), coord[3])) {
                    if (pluginConfiguration != null) {
                        pluginConfiguration = finalizeConfig(pluginConfiguration, mojoDescriptor);
                    }
                }
            }

            Session session = getInjector().getInstance(Session.class);
            Project project = getInjector().getInstance(Project.class);
            MojoExecution mojoExecution = getInjector().getInstance(MojoExecution.class);
            ExpressionEvaluator evaluator = new WrapEvaluator(
                    getInjector(), new PluginParameterExpressionEvaluatorV4(session, project, mojoExecution));

            EnhancedComponentConfigurator configurator = new EnhancedComponentConfigurator();
            configurator.configureComponent(
                    mojo, new XmlPlexusConfiguration(pluginConfiguration), evaluator, null, null);
            return mojo;
        } catch (Exception e) {
            throw new ParameterResolutionException("Unable to resolve mojo", e);
        }
    }

    /**
     * The @Mojo annotation is only retained in the class file, not at runtime,
     * so we need to actually read the class file with ASM to find the annotation and
     * the goal.
     */
    private static String getGoalFromMojoImplementationClass(Class<?> cl) throws IOException {
        return cl.getAnnotation(Named.class).value();
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void beforeEach(ExtensionContext context) throws Exception {
        if (pluginBasedir == null) {
            pluginBasedir = MavenDIExtension.getBasedir();
        }
        basedir = AnnotationSupport.findAnnotation(context.getElement().orElseThrow(), Basedir.class)
                .map(Basedir::value)
                .orElse(pluginBasedir);
        if (basedir != null) {
            if (basedir.isEmpty()) {
                basedir = pluginBasedir + "/target/tests/"
                        + context.getRequiredTestClass().getSimpleName() + "/"
                        + context.getRequiredTestMethod().getName();
            } else {
                basedir = basedir.replace("${basedir}", pluginBasedir);
            }
        }

        setContext(context);

        /*
           binder.install(ProviderMethodsModule.forObject(context.getRequiredTestInstance()));
           binder.requestInjection(context.getRequiredTestInstance());
           binder.bind(Log.class).toInstance(new DefaultLog(LoggerFactory.getLogger("anonymous")));
           binder.bind(ExtensionContext.class).toInstance(context);
           // Load maven 4 api Services interfaces and try to bind them to the (possible) mock instances
           // returned by the (possibly) mock InternalSession
           try {
               for (ClassPath.ClassInfo clazz :
                       ClassPath.from(getClassLoader()).getAllClasses()) {
                   if ("org.apache.maven.api.services".equals(clazz.getPackageName())) {
                       Class<?> load = clazz.load();
                       if (Service.class.isAssignableFrom(load)) {
                           Class<Service> svc = (Class) load;
                           binder.bind(svc).toProvider(() -> {
                               try {
                                   return getContainer()
                                           .lookup(InternalSession.class)
                                           .getService(svc);
                               } catch (ComponentLookupException e) {
                                   throw new RuntimeException("Unable to lookup service " + svc.getName());
                               }
                           });
                       }
                   }
               }
           } catch (Exception e) {
               throw new RuntimeException("Unable to bind session services", e);
           }

        */

        Path basedirPath = Paths.get(getBasedir());

        InjectMojo mojo = AnnotationSupport.findAnnotation(context.getElement().get(), InjectMojo.class)
                .orElse(null);
        Model defaultModel = Model.newBuilder()
                .groupId("myGroupId")
                .artifactId("myArtifactId")
                .version("1.0-SNAPSHOT")
                .packaging("jar")
                .build(Build.newBuilder()
                        .directory(basedirPath.resolve("target").toString())
                        .outputDirectory(basedirPath.resolve("target/classes").toString())
                        .sources(List.of(
                                Source.newBuilder()
                                        .scope("main")
                                        .lang("java")
                                        .directory(basedirPath
                                                .resolve("src/main/java")
                                                .toString())
                                        .build(),
                                Source.newBuilder()
                                        .scope("test")
                                        .lang("java")
                                        .directory(basedirPath
                                                .resolve("src/test/java")
                                                .toString())
                                        .build()))
                        .testOutputDirectory(
                                basedirPath.resolve("target/test-classes").toString())
                        .build())
                .build();
        Path[] modelPath = new Path[] {null};
        Model tmodel = null;
        if (mojo != null) {
            String pom = mojo.pom();
            if (pom != null && !pom.isEmpty()) {
                try (Reader r = openPomUrl(context.getRequiredTestClass(), pom, modelPath)) {
                    tmodel = new MavenStaxReader().read(r);
                }
            } else {
                Path pomPath = basedirPath.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    try (Reader r = Files.newBufferedReader(pomPath)) {
                        tmodel = new MavenStaxReader().read(r);
                        modelPath[0] = pomPath;
                    }
                }
            }
        }
        Model model;
        if (tmodel == null) {
            model = defaultModel;
        } else {
            model = new MavenMerger().merge(tmodel, defaultModel, false, null);
        }
        tmodel = new DefaultModelPathTranslator(new DefaultPathTranslator())
                .alignToBaseDirectory(tmodel, Paths.get(getBasedir()), null);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(Model.class, tmodel);

        // mojo execution
        // Map<Object, Object> map = getInjector().getContext().getContextData();
        PluginDescriptor pluginDescriptor;
        ClassLoader classLoader = context.getRequiredTestClass().getClassLoader();
        try (InputStream is = requireNonNull(
                        classLoader.getResourceAsStream(getPluginDescriptorLocation()),
                        "Unable to find plugin descriptor: " + getPluginDescriptorLocation());
                Reader reader = new BufferedReader(new XmlStreamReader(is))) {
            // new InterpolationFilterReader(reader, map, "${", "}");
            pluginDescriptor = new PluginDescriptorStaxReader().read(reader);
        }
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(PluginDescriptor.class, pluginDescriptor);
        // for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
        //    getContainer().addComponentDescriptor(desc);
        // }

        @SuppressWarnings({"unused", "MagicNumber"})
        class Foo {

            @Provides
            @Singleton
            @Priority(-10)
            private InternalSession createSession() {
                MojoTest mojoTest = context.getRequiredTestClass().getAnnotation(MojoTest.class);
                if (mojoTest != null && mojoTest.realSession()) {
                    // Try to create a real session using ApiRunner without compile-time dependency
                    try {
                        Class<?> apiRunner = Class.forName("org.apache.maven.impl.standalone.ApiRunner");
                        Object session = apiRunner.getMethod("createSession").invoke(null);
                        return (InternalSession) session;
                    } catch (Throwable t) {
                        // Explicit request: do not fall back; abort the test with details instead of mocking
                        throw new org.opentest4j.TestAbortedException(
                                "@MojoTest(realSession=true) requested but could not create a real session.", t);
                    }
                }
                return SessionMock.getMockSession(getBasedir());
            }

            @Provides
            @Singleton
            @Priority(-10)
            private Project createProject(InternalSession s) {
                ProjectStub stub = new ProjectStub();
                if (!"pom".equals(model.getPackaging())) {
                    ProducedArtifactStub artifact = new ProducedArtifactStub(
                            model.getGroupId(), model.getArtifactId(), "", model.getVersion(), model.getPackaging());
                    stub.setMainArtifact(artifact);
                }
                stub.setModel(model);
                stub.setBasedir(Paths.get(MojoExtension.getBasedir()));
                stub.setPomPath(modelPath[0]);
                s.getService(ArtifactManager.class).setPath(stub.getPomArtifact(), modelPath[0]);
                return stub;
            }

            @Provides
            @Singleton
            @Priority(-10)
            private MojoExecution createMojoExecution() {
                MojoExecutionStub mes = new MojoExecutionStub("executionId", null);
                if (mojo != null) {
                    String goal = mojo.goal();
                    int idx = goal.lastIndexOf(':');
                    if (idx >= 0) {
                        goal = goal.substring(idx + 1);
                    }
                    mes.setGoal(goal);
                    for (MojoDescriptor md : pluginDescriptor.getMojos()) {
                        if (goal.equals(md.getGoal())) {
                            mes.setDescriptor(md);
                        }
                    }
                    requireNonNull(mes.getDescriptor());
                }
                PluginStub plugin = new PluginStub();
                plugin.setDescriptor(pluginDescriptor);
                mes.setPlugin(plugin);
                return mes;
            }

            @Provides
            @Singleton
            @Priority(-10)
            private Log createLog() {
                return new DefaultLog(LoggerFactory.getLogger("anonymous"));
            }

            @Provides
            static RepositorySystemSupplier newRepositorySystemSupplier() {
                return new RepositorySystemSupplier();
            }

            @Provides
            static RepositorySystem newRepositorySystem(RepositorySystemSupplier repositorySystemSupplier) {
                return repositorySystemSupplier.getRepositorySystem();
            }

            @Provides
            @Priority(10)
            static RepositoryFactory newRepositoryFactory(Session session) {
                return session.getService(RepositoryFactory.class);
            }

            @Provides
            @Priority(10)
            static VersionParser newVersionParser(Session session) {
                return session.getService(VersionParser.class);
            }

            @Provides
            @Priority(10)
            static LocalRepositoryManager newLocalRepositoryManager(Session session) {
                return session.getService(LocalRepositoryManager.class);
            }

            @Provides
            @Priority(10)
            static ArtifactInstaller newArtifactInstaller(Session session) {
                return session.getService(ArtifactInstaller.class);
            }

            @Provides
            @Priority(10)
            static ArtifactDeployer newArtifactDeployer(Session session) {
                return session.getService(ArtifactDeployer.class);
            }

            @Provides
            @Priority(10)
            static ArtifactManager newArtifactManager(Session session) {
                return session.getService(ArtifactManager.class);
            }

            @Provides
            @Priority(10)
            static ProjectManager newProjectManager(Session session) {
                return session.getService(ProjectManager.class);
            }

            @Provides
            @Priority(10)
            static ArtifactFactory newArtifactFactory(Session session) {
                return session.getService(ArtifactFactory.class);
            }

            @Provides
            @Priority(10)
            static ProjectBuilder newProjectBuilder(Session session) {
                return session.getService(ProjectBuilder.class);
            }

            @Provides
            @Priority(10)
            static ModelXmlFactory newModelXmlFactory(Session session) {
                return session.getService(ModelXmlFactory.class);
            }
        }

        getInjector().bindInstance(Foo.class, new Foo());

        getInjector().injectInstance(context.getRequiredTestInstance());

        //        SessionScope sessionScope = getInjector().getInstance(SessionScope.class);
        //        sessionScope.enter();
        //        sessionScope.seed(Session.class, s);
        //        sessionScope.seed(InternalSession.class, s);

        //        MojoExecutionScope mojoExecutionScope = getInjector().getInstance(MojoExecutionScope.class);
        //        mojoExecutionScope.enter();
        //        mojoExecutionScope.seed(Project.class, p);
        //        mojoExecutionScope.seed(MojoExecution.class, me);
    }

    private Reader openPomUrl(Class<?> holder, String pom, Path[] modelPath) throws IOException {
        if (pom.startsWith("file:")) {
            Path path = Paths.get(getBasedir()).resolve(pom.substring("file:".length()));
            modelPath[0] = path;
            return Files.newBufferedReader(path);
        } else if (pom.startsWith("classpath:")) {
            URL url = holder.getResource(pom.substring("classpath:".length()));
            if (url == null) {
                throw new IllegalStateException("Unable to find pom on classpath: " + pom);
            }
            return new XmlStreamReader(url.openStream());
        } else if (pom.contains("<project>")) {
            return new StringReader(pom);
        } else {
            Path path = Paths.get(getBasedir()).resolve(pom);
            modelPath[0] = path;
            return Files.newBufferedReader(path);
        }
    }

    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    protected String[] mojoCoordinates(String goal) throws Exception {
        if (goal.matches(".*:.*:.*:.*")) {
            return goal.split(":");
        } else {
            Path pluginPom = Paths.get(getPluginBasedir(), "pom.xml");
            Xpp3Dom pluginPomDom = Xpp3DomBuilder.build(Files.newBufferedReader(pluginPom));
            String artifactId = pluginPomDom.getChild("artifactId").getValue();
            String groupId = resolveFromRootThenParent(pluginPomDom, "groupId");
            String version = resolveFromRootThenParent(pluginPomDom, "version");
            return new String[] {groupId, artifactId, version, goal};
        }
    }

    private XmlNode finalizeConfig(XmlNode config, MojoDescriptor mojoDescriptor) {
        List<XmlNode> children = new ArrayList<>();
        if (mojoDescriptor != null) {
            XmlNode defaultConfiguration;
            defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                XmlNode parameterConfiguration = config.child(parameter.getName());
                if (parameterConfiguration == null) {
                    parameterConfiguration = config.child(parameter.getAlias());
                }
                XmlNode parameterDefaults = defaultConfiguration.child(parameter.getName());
                parameterConfiguration = XmlNode.merge(parameterConfiguration, parameterDefaults, Boolean.TRUE);
                if (parameterConfiguration != null) {
                    Map<String, String> attributes = new HashMap<>(parameterConfiguration.attributes());
                    // if (isEmpty(parameterConfiguration.getAttribute("implementation"))
                    //         && !isEmpty(parameter.getImplementation())) {
                    //     attributes.put("implementation", parameter.getImplementation());
                    // }
                    parameterConfiguration = XmlNode.newInstance(
                            parameter.getName(),
                            parameterConfiguration.value(),
                            attributes,
                            parameterConfiguration.children(),
                            parameterConfiguration.inputLocation());

                    children.add(parameterConfiguration);
                }
            }
        }
        return XmlNode.newInstance("configuration", null, null, children, null);
    }

    private static Optional<Xpp3Dom> child(Xpp3Dom element, String name) {
        return Optional.ofNullable(element.getChild(name));
    }

    private static Stream<Xpp3Dom> children(Xpp3Dom element) {
        return Stream.of(element.getChildren());
    }

    public static XmlNode extractPluginConfiguration(String artifactId, Xpp3Dom pomDom) throws Exception {
        Xpp3Dom pluginConfigurationElement = child(pomDom, "build")
                .flatMap(buildElement -> child(buildElement, "plugins"))
                .map(MojoExtension::children)
                .orElseGet(Stream::empty)
                .filter(e -> e.getChild("artifactId").getValue().equals(artifactId))
                .findFirst()
                .flatMap(buildElement -> child(buildElement, "configuration"))
                .orElse(Xpp3DomBuilder.build(new StringReader("<configuration/>")));
        return pluginConfigurationElement.getDom();
    }

    /**
     * sometimes the parent element might contain the correct value so generalize that access
     *
     * TODO find out where this is probably done elsewhere
     */
    private static String resolveFromRootThenParent(Xpp3Dom pluginPomDom, String element) throws Exception {
        return Optional.ofNullable(child(pluginPomDom, element).orElseGet(() -> child(pluginPomDom, "parent")
                        .flatMap(e -> child(e, element))
                        .orElse(null)))
                .map(Xpp3Dom::getValue)
                .orElseThrow(() -> new Exception("unable to determine " + element));
    }

    /**
     * Convenience method to obtain the value of a variable on a mojo that might not have a getter.
     * <br>
     * NOTE: the caller is responsible for casting to what the desired type is.
     */
    public static Object getVariableValueFromObject(Object object, String variable) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     * <br>
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     */
    public static Map<String, Object> getVariablesAndValuesFromObject(Object object) throws IllegalAccessException {
        return getVariablesAndValuesFromObject(object.getClass(), object);
    }

    /**
     * Convenience method to obtain all variables and values from the mojo (including its superclasses)
     * <br>
     * Note: the values in the map are of type Object so the caller is responsible for casting to desired types.
     *
     * @return map of variable names and values
     */
    public static Map<String, Object> getVariablesAndValuesFromObject(Class<?> clazz, Object object)
            throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = clazz.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);
        for (Field field : fields) {
            map.put(field.getName(), field.get(object));
        }
        Class<?> superclass = clazz.getSuperclass();
        if (!Object.class.equals(superclass)) {
            map.putAll(getVariablesAndValuesFromObject(superclass, object));
        }
        return map;
    }

    /**
     * Convenience method to set values to variables in objects that don't have setters
     */
    public static void setVariableValueToObject(Object object, String variable, Object value)
            throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        requireNonNull(field, "Field " + variable + " not found");
        field.setAccessible(true);
        field.set(object, value);
    }

    static class WrapEvaluator implements TypeAwareExpressionEvaluator {

        private final Injector injector;
        private final TypeAwareExpressionEvaluator evaluator;

        WrapEvaluator(Injector injector, TypeAwareExpressionEvaluator evaluator) {
            this.injector = injector;
            this.evaluator = evaluator;
        }

        @Override
        public Object evaluate(String expression) throws ExpressionEvaluationException {
            return evaluate(expression, null);
        }

        @Override
        public Object evaluate(String expression, Class<?> type) throws ExpressionEvaluationException {
            Object value = evaluator.evaluate(expression, type);
            if (value == null) {
                String expr = stripTokens(expression);
                if (expr != null) {
                    try {
                        value = injector.getInstance(Key.of(type, expr));
                    } catch (DIException e) {
                        // nothing
                    }
                }
            }
            return value;
        }

        private String stripTokens(String expr) {
            if (expr.startsWith("${") && expr.endsWith("}")) {
                return expr.substring(2, expr.length() - 1);
            }
            return null;
        }

        @Override
        public File alignToBaseDirectory(File path) {
            return evaluator.alignToBaseDirectory(path);
        }
    }

    /*
    private Scope getScopeInstanceOrNull(final Injector injector, final Binding<?> binding) {
        return binding.acceptScopingVisitor(new DefaultBindingScopingVisitor<Scope>() {

            @Override
            public Scope visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
                throw new RuntimeException(String.format(
                        "I don't know how to handle the scopeAnnotation: %s", scopeAnnotation.getCanonicalName()));
            }

            @Override
            public Scope visitNoScoping() {
                if (binding instanceof LinkedKeyBinding) {
                    Binding<?> childBinding = injector.getBinding(((LinkedKeyBinding) binding).getLinkedKey());
                    return getScopeInstanceOrNull(injector, childBinding);
                }
                return null;
            }

            @Override
            public Scope visitEagerSingleton() {
                return Scopes.SINGLETON;
            }

            public Scope visitScope(Scope scope) {
                return scope;
            }
        });
    }*/

}
