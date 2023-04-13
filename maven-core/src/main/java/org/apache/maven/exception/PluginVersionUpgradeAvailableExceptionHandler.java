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
package org.apache.maven.exception;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public final class PluginVersionUpgradeAvailableExceptionHandler {
    private final ExceptionHandler defaultExceptionHandler;
    private final PluginVersionResolver pluginVersionResolver;

    public PluginVersionUpgradeAvailableExceptionHandler(
            ExceptionHandler defaultExceptionHandler, PluginVersionResolver pluginVersionResolver) {
        this.defaultExceptionHandler = defaultExceptionHandler;
        this.pluginVersionResolver = pluginVersionResolver;
    }

    public ExceptionSummary handleException(
            Throwable exception, RepositorySystemSession session, List<RemoteRepository> repositories) {
        ExceptionSummary summary = defaultExceptionHandler.handleException(exception);
        if (!(exception.getCause() instanceof MojoExecutionException)) {
            return summary;
        }

        Optional<String> suggestion =
                getNewestVersionAvailable((MojoExecutionException) exception.getCause(), session, repositories);

        if (suggestion.isPresent()) {
            ExceptionSummary possibleUpgrade = new ExceptionSummary(exception, suggestion.get(), "");
            List<ExceptionSummary> children = Stream.concat(summary.getChildren().stream(), Stream.of(possibleUpgrade))
                    .collect(Collectors.toList());
            summary = new ExceptionSummary(
                    summary.getException(), summary.getMessage(), summary.getReference(), children);
        }
        return summary;
    }

    private Optional<String> getNewestVersionAvailable(
            MojoExecutionException exception, RepositorySystemSession session, List<RemoteRepository> repositories) {
        String suggestion = null;
        try {
            if (exception.getPlugin().isPresent()) {
                Plugin plugin = exception.getPlugin().get();
                PluginVersionResult result =
                        pluginVersionResolver.resolve(new DefaultPluginVersionRequest(plugin, session, repositories));
                if (plugin.getVersion() != null && !plugin.getVersion().equals(result.getVersion())) {
                    suggestion = String.format("Consider upgrading to version '%s'", result.getVersion());
                }
            }
        } catch (PluginVersionResolutionException ex) {
            // Failed to resolve plugin, seems unlikely as it has been resolved in the first phases, silently continue.
        }

        return Optional.ofNullable(suggestion);
    }
}
