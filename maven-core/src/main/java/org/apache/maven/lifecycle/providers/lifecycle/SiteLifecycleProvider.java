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
 * Site lifecycle provider.
 *
 * @since 3.10.0
 */
@Singleton
@Named(SiteLifecycleProvider.NAME)
public class SiteLifecycleProvider extends AbstractLifecycleProvider {
    public static final String NAME = "site";

    private static final String[] PHASES = {"pre-site", "site", "post-site", "site-deploy"};

    private static final String[] PLUGIN_BINDINGS = {
        "site",
        "org.apache.maven.plugins:maven-site-plugin:" + PluginVersions.SITE_PLUGIN_VERSION + ":site",
        "site-deploy",
        "org.apache.maven.plugins:maven-site-plugin:" + PluginVersions.SITE_PLUGIN_VERSION + ":deploy"
    };

    public SiteLifecycleProvider() {
        super(NAME, PHASES, PLUGIN_BINDINGS);
    }
}
