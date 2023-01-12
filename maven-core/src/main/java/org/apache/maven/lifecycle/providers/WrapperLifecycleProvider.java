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
 * {@code wrapper} lifecycle provider.
 */
@Named(WrapperLifecycleProvider.LIFECYCLE_ID)
@Singleton
public final class WrapperLifecycleProvider extends AbstractLifecycleProvider {
    protected static final String LIFECYCLE_ID = "wrapper";

    // START SNIPPET: wrapper
    private static final String[] PHASES = {"wrapper"};

    private static final String[] BINDINGS = {"wrapper", "org.apache.maven.plugins:maven-wrapper-plugin:3.1.1:wrapper"};
    // END SNIPPET: wrapper

    @Inject
    public WrapperLifecycleProvider() {
        super(LIFECYCLE_ID, PHASES, BINDINGS);
    }
}
