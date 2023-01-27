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
package org.apache.maven.lifecycle.providers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code default} lifecycle provider.
 */
@Named(DefaultLifecycleProvider.LIFECYCLE_ID)
@Singleton
public final class DefaultLifecycleProvider extends AbstractLifecycleProvider {
    static final String LIFECYCLE_ID = "default";

    // START SNIPPET: default
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
    // END SNIPPET: default

    @Inject
    public DefaultLifecycleProvider() {
        super(
                LIFECYCLE_ID,
                PHASES,
                null // no global plugin bindings for default lifecycle: they are defined per-packaging in separate
                // providers
                );
    }
}
