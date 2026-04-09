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
package org.apache.maven.lifecycle.providers.lifecycle;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.providers.PluginVersions;

/**
 * Clean lifecycle provider.
 *
 * @since 3.10.0
 */
@Singleton
@Named(CleanLifecycleProvider.NAME)
public class CleanLifecycleProvider extends AbstractLifecycleProvider {
    public static final String NAME = "clean";

    private static final String[] PHASES = {"pre-clean", "clean", "post-clean"};

    private static final String[] PLUGIN_BINDINGS = {
        "clean", "org.apache.maven.plugins:maven-clean-plugin:" + PluginVersions.CLEAN_PLUGIN_VERSION + ":clean"
    };

    public CleanLifecycleProvider() {
        super(NAME, PHASES, PLUGIN_BINDINGS);
    }
}
