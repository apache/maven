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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.providers.PluginVersions;

/**
 * {@code pom} packaging plugins bindings provider for {@code default} lifecycle.
 *
 * @since 3.10.0
 */
@Named("pom")
@Singleton
public final class PomLifecycleMappingProvider extends AbstractLifecycleMappingProvider {
    // START SNIPPET: pom
    private static final String[] BINDINGS = {
        "install",
                "org.apache.maven.plugins:maven-install-plugin:" + PluginVersions.INSTALL_PLUGIN_VERSION + ":install",
        "deploy", "org.apache.maven.plugins:maven-deploy-plugin:" + PluginVersions.DEPLOY_PLUGIN_VERSION + ":deploy"
    };
    // END SNIPPET: pom

    public PomLifecycleMappingProvider() {
        super(BINDINGS);
    }
}
