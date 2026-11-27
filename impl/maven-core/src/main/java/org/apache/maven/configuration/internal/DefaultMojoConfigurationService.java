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
import javax.inject.Named;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.plugin.descriptor.Parameter;
import org.apache.maven.api.services.MojoConfigurationService;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.DefaultProject;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.sisu.Typed;

@Named
@Typed({MojoConfigurationService.class, Service.class})
@SessionScoped
public class DefaultMojoConfigurationService implements MojoConfigurationService {

    private Object resolveParameterValue(
            EnhancedCompositeBeanHelper helper,
            Class<?> mojoClass,
            String parameterName,
            PlexusConfiguration configuration)
            throws ComponentConfigurationException {

        Type propertyType = helper.getPropertyType(mojoClass, parameterName);

        if (MojoParameterType.isSafe(propertyType)) {
            return helper.getPropertyValue(mojoClass, parameterName, null, configuration);
        }

        if (configuration instanceof XmlPlexusConfiguration xmlConfiguration) {
            return xmlConfiguration.toXmlNode();
        }

        return XmlNode.newInstance(
                configuration.getName(),
                configuration.getValue(configuration.getAttribute("default-value")),
                null,
                null,
                null);
    }

    private final InternalMavenSession session;
    private final MavenPluginManager pluginManager;

    @Inject
    public DefaultMojoConfigurationService(InternalMavenSession session, MavenPluginManager pluginManager) {
        this.session = session;
        this.pluginManager = pluginManager;
    }

    @Override
    public Map<String, Object> getParameters(Project project, MojoExecution mojoExecution) {
        if (!(mojoExecution instanceof DefaultMojoExecution execution)) {
            throw new IllegalArgumentException("Unsupported MojoExecution implementation: "
                    + mojoExecution.getClass().getName());
        }

        if (!(project instanceof DefaultProject defaultProject)) {
            throw new IllegalArgumentException(
                    "Unsupported Project implementation: " + project.getClass().getName());
        }

        org.apache.maven.project.MavenProject mavenProject = defaultProject.getProject();

        org.apache.maven.plugin.MojoExecution delegate = execution.getDelegate();
        org.apache.maven.plugin.descriptor.MojoDescriptor mojoDescriptor = delegate.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        org.apache.maven.execution.MavenSession interpolationSession =
                session.getMavenSession().clone();
        interpolationSession.setCurrentProject(mavenProject);
        ClassRealm pluginRealm = pluginDescriptor.getClassRealm();

        if (pluginRealm == null) {
            try {
                pluginManager.setupPluginRealm(pluginDescriptor, interpolationSession, null, null, null);
                pluginRealm = pluginDescriptor.getClassRealm();
            } catch (PluginResolutionException | PluginContainerException e) {
                throw new IllegalStateException("Could not resolve plugin realm", e);
            }
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(pluginRealm);

            Class<?> mojoClass = Class.forName(mojoDescriptor.getImplementation(), false, pluginRealm);

            ExpressionEvaluator evaluator;
            if (mojoDescriptor.isV4Api()) {
                evaluator = new PluginParameterExpressionEvaluatorV4(session, project, mojoExecution);
            } else {
                evaluator = new PluginParameterExpressionEvaluator(interpolationSession, delegate);
            }

            PlexusConfiguration configuration = new DefaultPlexusConfiguration("configuration");

            if (mojoExecution.getConfiguration().isPresent()) {
                configuration = XmlPlexusConfiguration.toPlexusConfiguration(
                        mojoExecution.getConfiguration().get());
            }
            configuration = PluginConfigurationInterpolator.interpolateConfiguration(
                    configuration, evaluator, interpolationSession);

            EnhancedCompositeBeanHelper helper =
                    new EnhancedCompositeBeanHelper(new EnhancedConverterLookup(), pluginRealm, evaluator, null);

            Map<String, Object> parameters = new LinkedHashMap<>();
            Set<String> configuredParameters = new HashSet<>();

            for (int i = 0; i < configuration.getChildCount(); i++) {
                PlexusConfiguration element = configuration.getChild(i);
                String propertyName = toPropertyName(element.getName());

                Parameter parameter = mojoExecution.getDescriptor().getParameters().stream()
                        .filter(p -> p.getName().equals(propertyName)
                                || (p.getAlias() != null
                                        && toPropertyName(p.getAlias()).equals(propertyName)))
                        .findFirst()
                        .orElse(null);

                if (parameter == null) {
                    continue;
                }

                configuredParameters.add(parameter.getName());

                Object value = resolveParameterValue(helper, mojoClass, parameter.getName(), element);

                if (value != null) {
                    parameters.put(parameter.getName(), value);
                }
            }

            for (Parameter parameter : mojoExecution.getDescriptor().getParameters()) {
                if (configuredParameters.contains(parameter.getName()) || parameter.getDefaultValue() == null) {
                    continue;
                }

                PlexusConfiguration defaultConfiguration = new DefaultPlexusConfiguration(parameter.getName());
                defaultConfiguration.setAttribute("default-value", parameter.getDefaultValue());

                Object value = resolveParameterValue(helper, mojoClass, parameter.getName(), defaultConfiguration);

                if (value != null) {
                    parameters.put(parameter.getName(), value);
                }
            }

            return parameters;
        } catch (ComponentConfigurationException | ClassNotFoundException e) {
            throw new IllegalStateException("Could not resolve Mojo parameters", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private String toPropertyName(String name) {
        StringBuilder result = new StringBuilder();
        boolean uppercaseNext = false;

        for (char c : name.toCharArray()) {
            if (c == '-' || c == '_') {
                uppercaseNext = true;
            } else {
                result.append(uppercaseNext ? Character.toUpperCase(c) : c);
                uppercaseNext = false;
            }
        }

        return result.toString();
    }
}
