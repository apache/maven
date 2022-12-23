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
package org.apache.maven.extension.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Objects;

import org.codehaus.plexus.PlexusContainer;

/**
 * CoreExportsProvider
 */
@Named
@Singleton
public class CoreExportsProvider implements Provider<CoreExports> {

    private final CoreExports exports;

    @Inject
    public CoreExportsProvider(PlexusContainer container) {
        this(new CoreExports(CoreExtensionEntry.discoverFrom(container.getContainerRealm())));
    }

    public CoreExportsProvider(CoreExports exports) {
        this.exports = Objects.requireNonNull(exports);
    }

    public CoreExports get() {
        return exports;
    }
}
