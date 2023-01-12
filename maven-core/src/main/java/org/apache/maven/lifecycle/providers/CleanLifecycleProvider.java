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
 * {@code clean} lifecycle provider.
 */
@Named(CleanLifecycleProvider.LIFECYCLE_ID)
@Singleton
public final class CleanLifecycleProvider extends AbstractLifecycleProvider {
    protected static final String LIFECYCLE_ID = "clean";

    // START SNIPPET: clean
    private static final String[] PHASES = {"pre-clean", "clean", "post-clean"};

    private static final String MAVEN_CLEAN_PLUGIN_VERSION = "3.2.0";

    private static final String[] BINDINGS = {
        "clean", "org.apache.maven.plugins:maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION + ":clean"
    };
    // END SNIPPET: clean

    @Inject
    public CleanLifecycleProvider() {
        super(LIFECYCLE_ID, PHASES, BINDINGS);
    }
}
