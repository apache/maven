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
package org.apache.maven.plugin.testing.junit5;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.internal.ProviderMethodsModule;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.ConfigurationException;
import org.apache.maven.plugin.testing.MojoLogWrapper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.testing.PlexusExtension;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * JUnit extension to help testing Mojos. The extension should be automatically registered
 * by adding the {@link org.apache.maven.api.plugin.testing.MojoTest} annotation on the test class.
 *
 * @see MojoTest
 * @see org.apache.maven.api.plugin.testing.InjectMojo
 * @see org.apache.maven.api.plugin.testing.MojoParameter
 */
public class MojoExtension extends PlexusExtension implements ParameterResolver {

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
            InjectMojo injectMojo = parameterContext
                    .findAnnotation(InjectMojo.class)
                    .orElseGet(() -> parameterContext.getDeclaringExecutable().getAnnotation(InjectMojo.class));

            Set<MojoParameter> mojoParameters =
                    new HashSet<>(parameterContext.findRepeatableAnnotations(MojoParameter.class));

            Optional.ofNullable(parameterContext.getDeclaringExecutable().getAnnotation(MojoParameter.class))
                    .ifPresent(mojoParameters::add);

            Optional.ofNullable(parameterContext.getDeclaringExecutable().getAnnotation(MojoParameters.class))
                    .map(MojoParameters::value)
                    .map(Arrays::asList)
                    .ifPresent(mojoParameters::addAll);

            Class<?> holder = parameterContext.getTarget().get().getClass();
            PluginDescriptor descriptor = extensionContext
                    .getStore(ExtensionContext.Namespace.GLOBAL)
                    .get(PluginDescriptor.class, PluginDescriptor.class);
            return lookupMojo(holder, injectMojo, mojoParameters, descriptor);
        } catch (Exception e) {
            throw new ParameterResolutionException("Unable to resolve parameter", e);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // TODO provide protected setters in PlexusExtension
        Field field = PlexusExtension.class.getDeclaredField("basedir");
        field.setAccessible(true);
        field.set(null, getBasedir());
        field = PlexusExtension.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(this, context);

        getContainer().addComponent(getContainer(), PlexusContainer.class.getName());

        ((DefaultPlexusContainer) getContainer()).addPlexusInjector(Collections.emptyList(), binder -> {
            binder.install(ProviderMethodsModule.forObject(context.getRequiredTestInstance()));
            binder.requestInjection(context.getRequiredTestInstance());
            binder.bind(Log.class).toInstance(new MojoLogWrapper(LoggerFactory.getLogger("anonymous")));
            binder.bind(MavenSession.class).toInstance(mockMavenSession());
            binder.bind(MojoExecution.class).toInstance(mockMojoExecution());
        });

        Map<Object, Object> map = getContainer().getContext().getContextData();

        ClassLoader classLoader = context.getRequiredTestClass().getClassLoader();
        try (InputStream is = Objects.requireNonNull(
                        classLoader.getResourceAsStream(getPluginDescriptorLocation()),
                        "Unable to find plugin descriptor: " + getPluginDescriptorLocation());
                Reader reader = new BufferedReader(new XmlStreamReader(is));
                InterpolationFilterReader interpolationReader = new InterpolationFilterReader(reader, map, "${", "}")) {

            PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build(interpolationReader);

            context.getStore(ExtensionContext.Namespace.GLOBAL).put(PluginDescriptor.class, pluginDescriptor);

            for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
                getContainer().addComponentDescriptor(desc);
            }
        }
    }

    /**
     * Default MojoExecution mock
     *
     * @return a MojoExecution mock
     */
    private MojoExecution mockMojoExecution() {
        return Mockito.mock(MojoExecution.class);
    }

    /**
     * Default MavenSession mock
     *
     * @return a MavenSession mock
     */
    private MavenSession mockMavenSession() {
        MavenSession session = Mockito.mock(MavenSession.class);
        Mockito.when(session.getUserProperties()).thenReturn(new Properties());
        Mockito.when(session.getSystemProperties()).thenReturn(new Properties());
        return session;
    }

    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/plugin.xml";
    }

    private Mojo lookupMojo(
            Class<?> holder,
            InjectMojo injectMojo,
            Collection<MojoParameter> mojoParameters,
            PluginDescriptor descriptor)
            throws Exception {
        String goal = injectMojo.goal();
        String pom = injectMojo.pom();
        String[] coord = mojoCoordinates(goal);
        Xpp3Dom pomDom;
        if (pom.startsWith("file:")) {
            Path path = Paths.get(getBasedir()).resolve(pom.substring("file:".length()));
            pomDom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(path.toFile()));
        } else if (pom.startsWith("classpath:")) {
            URL url = holder.getResource(pom.substring("classpath:".length()));
            if (url == null) {
                throw new IllegalStateException("Unable to find pom on classpath: " + pom);
            }
            pomDom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(url.openStream()));
        } else if (pom.contains("<project>")) {
            pomDom = Xpp3DomBuilder.build(new StringReader(pom));
        } else {
            Path path = Paths.get(getBasedir()).resolve(pom);
            pomDom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(path.toFile()));
        }
        Xpp3Dom pluginConfiguration = extractPluginConfiguration(coord[1], pomDom);
        if (!mojoParameters.isEmpty()) {
            List<Xpp3Dom> children = mojoParameters.stream()
                    .map(mp -> {
                        Xpp3Dom c = new Xpp3Dom(mp.name());
                        c.setValue(mp.value());
                        return c;
                    })
                    .collect(Collectors.toList());
            Xpp3Dom config = new Xpp3Dom("configuration");
            children.forEach(config::addChild);
            pluginConfiguration = Xpp3Dom.mergeXpp3Dom(config, pluginConfiguration);
        }
        Mojo mojo = lookupMojo(coord, pluginConfiguration, descriptor);
        return mojo;
    }

    protected String[] mojoCoordinates(String goal) throws Exception {
        if (goal.matches(".*:.*:.*:.*")) {
            return goal.split(":");
        } else {
            Path pluginPom = Paths.get(getBasedir(), "pom.xml");
            Xpp3Dom pluginPomDom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(pluginPom.toFile()));
            String artifactId = pluginPomDom.getChild("artifactId").getValue();
            String groupId = resolveFromRootThenParent(pluginPomDom, "groupId");
            String version = resolveFromRootThenParent(pluginPomDom, "version");
            return new String[] {groupId, artifactId, version, goal};
        }
    }

    /**
     * lookup the mojo while we have all the relevent information
     */
    protected Mojo lookupMojo(String[] coord, Xpp3Dom pluginConfiguration, PluginDescriptor descriptor)
            throws Exception {
        // pluginkey = groupId : artifactId : version : goal
        Mojo mojo = lookup(Mojo.class, coord[0] + ":" + coord[1] + ":" + coord[2] + ":" + coord[3]);
        for (MojoDescriptor mojoDescriptor : descriptor.getMojos()) {
            if (Objects.equals(
                    mojoDescriptor.getImplementation(), mojo.getClass().getName())) {
                if (pluginConfiguration != null) {
                    pluginConfiguration = finalizeConfig(pluginConfiguration, mojoDescriptor);
                }
            }
        }
        if (pluginConfiguration != null) {
            MavenSession session = getContainer().lookup(MavenSession.class);
            MavenProject project;
            try {
                project = getContainer().lookup(MavenProject.class);
            } catch (ComponentLookupException e) {
                project = null;
            }
            MojoExecution mojoExecution;
            try {
                mojoExecution = getContainer().lookup(MojoExecution.class);
            } catch (ComponentLookupException e) {
                mojoExecution = null;
            }
            ExpressionEvaluator evaluator =
                    new WrapEvaluator(getContainer(), new PluginParameterExpressionEvaluator(session, mojoExecution));
            ComponentConfigurator configurator = new BasicComponentConfigurator();
            configurator.configureComponent(
                    mojo,
                    new XmlPlexusConfiguration(pluginConfiguration),
                    evaluator,
                    getContainer().getContainerRealm());
        }

        mojo.setLog(getContainer().lookup(Log.class));

        return mojo;
    }

    private Xpp3Dom finalizeConfig(Xpp3Dom config, MojoDescriptor mojoDescriptor) {
        List<Xpp3Dom> children = new ArrayList<>();
        if (mojoDescriptor != null && mojoDescriptor.getParameters() != null) {
            Xpp3Dom defaultConfiguration = MojoDescriptorCreator.convert(mojoDescriptor);
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = config.getChild(parameter.getName());
                if (parameterConfiguration == null) {
                    parameterConfiguration = config.getChild(parameter.getAlias());
                }
                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());
                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);
                if (parameterConfiguration != null) {
                    if (isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && !isEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }
                    children.add(parameterConfiguration);
                }
            }
        }
        Xpp3Dom c = new Xpp3Dom("configuration");
        children.forEach(c::addChild);
        return c;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static Optional<Xpp3Dom> child(Xpp3Dom element, String name) {
        return Optional.ofNullable(element.getChild(name));
    }

    private static Stream<Xpp3Dom> children(Xpp3Dom element) {
        return Stream.of(element.getChildren());
    }

    public static Xpp3Dom extractPluginConfiguration(String artifactId, Xpp3Dom pomDom) throws Exception {
        Xpp3Dom pluginConfigurationElement = child(pomDom, "build")
                .flatMap(buildElement -> child(buildElement, "plugins"))
                .map(MojoExtension::children)
                .orElseGet(Stream::empty)
                .filter(e -> e.getChild("artifactId").getValue().equals(artifactId))
                .findFirst()
                .flatMap(buildElement -> child(buildElement, "configuration"))
                .orElseThrow(
                        () -> new ConfigurationException("Cannot find a configuration element for a plugin with an "
                                + "artifactId of " + artifactId + "."));
        return pluginConfigurationElement;
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
     * Note: the caller is responsible for casting to what the desired type is.
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
        Objects.requireNonNull(field, "Field " + variable + " not found");
        field.setAccessible(true);
        field.set(object, value);
    }

    static class WrapEvaluator implements TypeAwareExpressionEvaluator {

        private final PlexusContainer container;
        private final TypeAwareExpressionEvaluator evaluator;

        WrapEvaluator(PlexusContainer container, TypeAwareExpressionEvaluator evaluator) {
            this.container = container;
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
                        value = container.lookup(type, expr);
                    } catch (ComponentLookupException e) {
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
}
