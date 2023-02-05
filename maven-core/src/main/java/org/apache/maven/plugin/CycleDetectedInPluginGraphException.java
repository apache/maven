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

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;

/**
 * Exception occurring trying to resolve a plugin.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CycleDetectedInPluginGraphException extends Exception {
    private final Plugin plugin;

    public CycleDetectedInPluginGraphException(Plugin plugin, CycleDetectedInComponentGraphException e) {
        super("A cycle was detected in the component graph of the plugin: " + plugin.getArtifactId());

        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
