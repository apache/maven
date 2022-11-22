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
 * {@code site} lifecycle provider.
 */
@Named(SiteLifecycleProvider.LIFECYCLE_ID)
@Singleton
public final class SiteLifecycleProvider extends AbstractLifecycleProvider {
    protected static final String LIFECYCLE_ID = "site";

    // START SNIPPET: site
    private static final String[] PHASES = {"pre-site", "site", "post-site", "site-deploy"};

    private static final String[] BINDINGS = {
        "site", "org.apache.maven.plugins:maven-site-plugin:3.9.1:site",
        "site-deploy", "org.apache.maven.plugins:maven-site-plugin:3.9.1:deploy"
    };
    // END SNIPPET: site

    @Inject
    public SiteLifecycleProvider() {
        super(LIFECYCLE_ID, PHASES, BINDINGS);
    }
}
