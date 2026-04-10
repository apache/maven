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

/**
 * Default lifecycle provider.
 *
 * @since 3.10.0
 */
@Singleton
@Named(DefaultLifecycleProvider.NAME)
public class DefaultLifecycleProvider extends AbstractLifecycleProvider {
    public static final String NAME = "default";

    private static final String[] PHASES = {
        "validate",
        "initialize",
        "generate-sources",
        "process-sources",
        "generate-resources",
        "process-resources",
        "compile",
        "process-classes",
        "generate-test-sources",
        "process-test-sources",
        "generate-test-resources",
        "process-test-resources",
        "test-compile",
        "process-test-classes",
        "test",
        "prepare-package",
        "package",
        "pre-integration-test",
        "integration-test",
        "post-integration-test",
        "verify",
        "install",
        "deploy"
    };

    private static final String[] PLUGIN_BINDINGS = {};

    public DefaultLifecycleProvider() {
        super(NAME, PHASES, PLUGIN_BINDINGS);
    }
}
