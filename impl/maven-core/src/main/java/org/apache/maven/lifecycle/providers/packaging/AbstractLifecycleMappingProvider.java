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
package org.apache.maven.lifecycle.providers.packaging;

import javax.inject.Provider;

import java.util.Collections;
import java.util.HashMap;

import org.apache.maven.lifecycle.PluginVersions;
import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

import static java.util.Objects.requireNonNull;

/**
 * Base lifecycle mapping provider, ie per-packaging plugin bindings for {@code default} lifecycle.
 */
public abstract class AbstractLifecycleMappingProvider implements Provider<LifecycleMapping> {
    // START SNIPPET: versions
    /** @deprecated Use {@link PluginVersions#RESOURCES} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String RESOURCES_PLUGIN_VERSION = PluginVersions.RESOURCES;

    /** @deprecated Use {@link PluginVersions#COMPILER} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String COMPILER_PLUGIN_VERSION = PluginVersions.COMPILER;

    /** @deprecated Use {@link PluginVersions#SUREFIRE} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String SUREFIRE_PLUGIN_VERSION = PluginVersions.SUREFIRE;

    /** @deprecated Use {@link PluginVersions#INSTALL} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String INSTALL_PLUGIN_VERSION = PluginVersions.INSTALL;

    /** @deprecated Use {@link PluginVersions#DEPLOY} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String DEPLOY_PLUGIN_VERSION = PluginVersions.DEPLOY;

    // packaging

    /** @deprecated Use {@link PluginVersions#JAR} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String JAR_PLUGIN_VERSION = PluginVersions.JAR;

    /** @deprecated Use {@link PluginVersions#EAR} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String EAR_PLUGIN_VERSION = PluginVersions.EAR;

    /** @deprecated Use {@link PluginVersions#EJB} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String EJB_PLUGIN_VERSION = PluginVersions.EJB;

    /** @deprecated Use {@link PluginVersions#PLUGIN} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String PLUGIN_PLUGIN_VERSION = PluginVersions.PLUGIN;

    /** @deprecated Use {@link PluginVersions#RAR} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String RAR_PLUGIN_VERSION = PluginVersions.RAR;

    /** @deprecated Use {@link PluginVersions#WAR} instead. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    protected static final String WAR_PLUGIN_VERSION = PluginVersions.WAR;
    // END SNIPPET: versions

    private final LifecycleMapping lifecycleMapping;

    protected AbstractLifecycleMappingProvider(String[] pluginBindings) {
        requireNonNull(pluginBindings);
        final int len = pluginBindings.length;
        if (len < 2 || len % 2 != 0) {
            throw new IllegalArgumentException("Plugin bindings must have more than 0, even count of elements");
        }

        HashMap<String, LifecyclePhase> lifecyclePhaseBindings = new HashMap<>(len / 2);
        for (int i = 0; i < len; i = i + 2) {
            lifecyclePhaseBindings.put(pluginBindings[i], new LifecyclePhase(pluginBindings[i + 1]));
        }

        Lifecycle lifecycle = new Lifecycle();
        lifecycle.setId("default");
        lifecycle.setLifecyclePhases(Collections.unmodifiableMap(lifecyclePhaseBindings));

        this.lifecycleMapping = new DefaultLifecycleMapping(Collections.singletonList(lifecycle));
    }

    @Override
    public LifecycleMapping get() {
        return lifecycleMapping;
    }
}
