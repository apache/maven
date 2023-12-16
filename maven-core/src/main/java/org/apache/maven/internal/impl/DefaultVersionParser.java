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

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.VersionParserException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionScheme;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * A wrapper class around a resolver version that works as model version parser as well.
 */
@Named
@Singleton
public class DefaultVersionParser implements VersionParser, org.apache.maven.model.version.VersionParser {
    private static final String SNAPSHOT = "SNAPSHOT";
    private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile("^(.*-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

    private final Provider<VersionScheme> versionSchemeProvider;

    @Inject
    public DefaultVersionParser(Provider<VersionScheme> versionSchemeProvider) {
        this.versionSchemeProvider = nonNull(versionSchemeProvider, "versionSchemeProvider");
    }

    @Override
    public Version parseVersion(String version) {
        nonNull(version, "version");
        return new DefaultVersion(versionSchemeProvider.get(), version);
    }

    @Override
    public VersionRange parseVersionRange(String range) {
        nonNull(range, "range");
        return new DefaultVersionRange(versionSchemeProvider.get(), range);
    }

    @Override
    public boolean isSnapshot(String version) {
        return checkSnapshot(version);
    }

    static boolean checkSnapshot(String version) {
        return version.endsWith(SNAPSHOT) || SNAPSHOT_TIMESTAMP.matcher(version).matches();
    }

    static class DefaultVersion implements Version {
        private final VersionScheme versionScheme;
        private final org.eclipse.aether.version.Version delegate;

        DefaultVersion(VersionScheme versionScheme, String delegateValue) {
            this.versionScheme = versionScheme;
            try {
                this.delegate = versionScheme.parseVersion(delegateValue);
            } catch (InvalidVersionSpecificationException e) {
                throw new VersionParserException("Unable to parse version: " + delegateValue, e);
            }
        }

        @Override
        public int compareTo(Version o) {
            if (o instanceof DefaultVersion) {
                return delegate.compareTo(((DefaultVersion) o).delegate);
            } else {
                return compareTo(new DefaultVersion(versionScheme, o.asString()));
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
        private final VersionScheme versionScheme;
        private final org.eclipse.aether.version.VersionRange delegate;

        DefaultVersionRange(VersionScheme versionScheme, String delegateValue) {
            this.versionScheme = versionScheme;
            try {
                this.delegate = versionScheme.parseVersionRange(delegateValue);
            } catch (InvalidVersionSpecificationException e) {
                throw new VersionParserException("Unable to parse version range: " + delegateValue, e);
            }
        }

        @Override
        public boolean contains(Version version) {
            if (version instanceof DefaultVersion) {
                return delegate.containsVersion(((DefaultVersion) version).delegate);
            } else {
                return contains(new DefaultVersion(versionScheme, version.asString()));
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
