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
package org.apache.maven.internal.impl.resolver;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

/**
 * Default version scheme provider: provides singleton {@link GenericVersionScheme} instance.
 */
@Singleton
@Named
public class MavenVersionScheme implements VersionScheme {

    private final VersionScheme delegate = new GenericVersionScheme();

    @Override
    public Version parseVersion(String version) throws InvalidVersionSpecificationException {
        return delegate.parseVersion(version);
    }

    @Override
    public VersionRange parseVersionRange(String range) throws InvalidVersionSpecificationException {
        return delegate.parseVersionRange(range);
    }

    @Override
    public VersionConstraint parseVersionConstraint(String constraint) throws InvalidVersionSpecificationException {
        return delegate.parseVersionConstraint(constraint);
    }
}
