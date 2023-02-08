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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultVersionParser implements VersionParser {
    private static final String SNAPSHOT = "SNAPSHOT";
    private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile("^(.*-)?(\\d{8}\\.\\d{6}-\\d+)$");

    @Override
    public Version parseVersion(String version) {
        return new DefaultVersion(new DefaultArtifactVersion(nonNull(version, "version")));
    }

    @Override
    public VersionRange parseVersionRange(String range) {
        try {
            return new DefaultVersionRange(createFromVersionSpec(nonNull(range, "version")));
        } catch (InvalidVersionSpecificationException e) {
            throw new VersionParserException("Unable to parse version range: " + range, e);
        }
    }

    @Override
    public boolean isSnapshot(String version) {
        return checkSnapshot(version);
    }

    static boolean checkSnapshot(String version) {
        return version.endsWith(SNAPSHOT) || SNAPSHOT_TIMESTAMP.matcher(version).matches();
    }

    static class DefaultVersion implements Version {
        private final ArtifactVersion delegate;

        DefaultVersion(ArtifactVersion delegate) {
            this.delegate = delegate;
        }

        @Override
        public int compareTo(Version o) {
            if (o instanceof DefaultVersion) {
                return delegate.compareTo(((DefaultVersion) o).delegate);
            } else {
                return delegate.compareTo(new DefaultArtifactVersion(o.toString()));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultVersion that = (DefaultVersion) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        @Override
        public String asString() {
            return delegate.toString();
        }

        @Override
        public String toString() {
            return asString();
        }
    }

    static class DefaultVersionRange implements VersionRange {
        private final org.apache.maven.artifact.versioning.VersionRange delegate;

        DefaultVersionRange(org.apache.maven.artifact.versioning.VersionRange delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean contains(Version version) {
            if (version instanceof DefaultVersion) {
                return delegate.containsVersion(((DefaultVersion) version).delegate);
            } else {
                return delegate.containsVersion(new DefaultArtifactVersion(version.toString()));
            }
        }

        @Override
        public String asString() {
            return delegate.toString();
        }

        @Override
        public String toString() {
            return asString();
        }
    }
}
