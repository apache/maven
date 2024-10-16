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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Plugin;

/**
 * Exception occurring trying to resolve a plugin.
 *
 */
public class PluginResolutionException extends Exception {

    private final Plugin plugin;

    public PluginResolutionException(Plugin plugin, Throwable cause) {
        super(
                "Plugin " + plugin.getId() + " or one of its dependencies could not be resolved: " + cause.getMessage(),
                cause);
        this.plugin = plugin;
    }

    public PluginResolutionException(Plugin plugin, List<Exception> exceptions, Throwable cause) {
        super(
                "Plugin " + plugin.getId() + " or one of its dependencies could not be resolved:"
                        + System.lineSeparator() + "\t"
                        + exceptions.stream()
                                .map(Throwable::getMessage)
                                .collect(Collectors.joining(System.lineSeparator() + "\t")),
                cause);
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
