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
package org.apache.maven.lifecycle.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.stream;

/**
 * @since 3.3.1, MNG-5753
 */
@Named
@Singleton
public class DefaultMojoExecutionConfigurator implements MojoExecutionConfigurator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void configure(MavenProject project, MojoExecution mojoExecution, boolean allowPluginLevelConfig) {
        String g = mojoExecution.getPlugin().getGroupId();

        String a = mojoExecution.getPlugin().getArtifactId();

        Plugin plugin = findPlugin(g, a, project.getBuildPlugins());

        if (plugin == null && project.getPluginManagement() != null) {
            plugin = findPlugin(g, a, project.getPluginManagement().getPlugins());
        }

        if (plugin != null) {
            PluginExecution pluginExecution =
                    findPluginExecution(mojoExecution.getExecutionId(), plugin.getExecutions());

            XmlNode pomConfiguration = null;

            if (pluginExecution != null) {
                pomConfiguration = pluginExecution.getDelegate().getConfiguration();
            } else if (allowPluginLevelConfig) {
                pomConfiguration = plugin.getDelegate().getConfiguration();
            }

            XmlNode mojoConfiguration = mojoExecution.getConfiguration() != null
                    ? mojoExecution.getConfiguration().getDom()
                    : null;

            XmlNode mergedConfiguration = XmlNodeImpl.merge(mojoConfiguration, pomConfiguration);

            mojoExecution.setConfiguration(mergedConfiguration);

            checkUnknownMojoConfigurationParameters(mojoExecution);
        }
    }

    private Plugin findPlugin(String groupId, String artifactId, Collection<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if (artifactId.equals(plugin.getArtifactId()) && groupId.equals(plugin.getGroupId())) {
                return plugin;
            }
        }

        return null;
    }

    private PluginExecution findPluginExecution(String executionId, Collection<PluginExecution> executions) {
        if (executionId != null && !executionId.isEmpty()) {
            for (PluginExecution execution : executions) {
                if (executionId.equals(execution.getId())) {
                    return execution;
                }
            }
        }

        return null;
    }

    private void checkUnknownMojoConfigurationParameters(MojoExecution mojoExecution) {
        if (mojoExecution.getConfiguration() == null
                || mojoExecution.getConfiguration().getChildCount() == 0) {
            return;
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        // in first step get parameter names of current goal
        Set<String> parametersNamesGoal = mojoDescriptor.getParameters().stream()
                .flatMap(this::getParameterNames)
                .collect(Collectors.toSet());

        Set<String> unknownParameters = getUnknownParameters(mojoExecution, parametersNamesGoal);

        if (unknownParameters.isEmpty()) {
            return;
        }

        // second step get parameter names of all plugin goals
        Set<String> parametersNamesAll = mojoDescriptor.getPluginDescriptor().getMojos().stream()
                .flatMap(m -> m.getParameters().stream())
                .flatMap(this::getParameterNames)
                .collect(Collectors.toSet());

        unknownParameters = getUnknownParameters(mojoExecution, parametersNamesAll);

        unknownParameters.forEach(name -> {
            MessageBuilder messageBuilder = MessageUtils.buffer()
                    .warning("Parameter '")
                    .warning(name)
                    .warning("' is unknown for plugin '")
                    .warning(mojoExecution.getArtifactId())
                    .warning(":")
                    .warning(mojoExecution.getVersion())
                    .warning(":")
                    .warning(mojoExecution.getGoal());

            if (mojoExecution.getExecutionId() != null) {
                messageBuilder.warning(" (");
                messageBuilder.warning(mojoExecution.getExecutionId());
                messageBuilder.warning(")");
            }

            messageBuilder.warning("'");

            logger.warn(messageBuilder.toString());
        });
    }

    private Stream<String> getParameterNames(Parameter parameter) {
        if (parameter.getAlias() != null) {
            return Stream.of(parameter.getName(), parameter.getAlias());
        } else {
            return Stream.of(parameter.getName());
        }
    }

    private Set<String> getUnknownParameters(MojoExecution mojoExecution, Set<String> parameters) {
        return stream(mojoExecution.getConfiguration().getChildren())
                .map(x -> x.getName())
                .filter(name -> !parameters.contains(name))
                .collect(Collectors.toSet());
    }
}
