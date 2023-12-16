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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Map;

import org.eclipse.aether.internal.impl.version.GenericVersionSchemeProvider;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.version.*;
import org.eclipse.sisu.Priority;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * A Maven core component that provides {@link VersionScheme} instances based on session configuration.
 */
@Named
@Singleton
@Priority(10)
public class SessionVersionSchemeProvider implements Provider<VersionScheme> {
    private final Provider<InternalSession> internalSessionProvider;
    private final Map<String, VersionScheme> versionSchemes;

    @Inject
    public SessionVersionSchemeProvider(
            Provider<InternalSession> internalSessionProvider, Map<String, VersionScheme> versionSchemes) {
        this.internalSessionProvider = nonNull(internalSessionProvider, "internalSessionProvider");
        this.versionSchemes = nonNull(versionSchemes, "versionSchemes");
    }

    @Override
    public VersionScheme get() {
        InternalSession session = internalSessionProvider.get();
        String schemeName = ConfigUtils.getString(
                session.getSession(), GenericVersionSchemeProvider.NAME, "maven.versionScheme.name");
        VersionScheme result = versionSchemes.get(schemeName);
        if (result == null) {
            // A "small hack" here: if we'd throw IAEx here, it would be caught by Guice and user would end up
            // with cryptic error how object graph cannot be constructed. Hence, what we do instead is
            // returning "fake" VersionScheme instance, that just throws, thus postponing failure to the point
            // where scheme is really needed, and allowing Guice to construct Maven components. This would allow
            // us to catch exception and present (more) meaningful message to end user.
            result = new VersionScheme() {
                @Override
                public Version parseVersion(String s) {
                    throw new IllegalArgumentException("Unsupported version scheme: " + schemeName);
                }

                @Override
                public VersionRange parseVersionRange(String s) {
                    throw new IllegalArgumentException("Unsupported version scheme: " + schemeName);
                }

                @Override
                public VersionConstraint parseVersionConstraint(String s) {
                    throw new IllegalArgumentException("Unsupported version scheme: " + schemeName);
                }
            };
        }
        return result;
    }
}
