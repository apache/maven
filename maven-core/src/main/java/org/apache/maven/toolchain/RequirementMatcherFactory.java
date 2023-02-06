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
package org.apache.maven.toolchain;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 *
 * @author mkleint
 */
public final class RequirementMatcherFactory {
    private RequirementMatcherFactory() {}

    public static RequirementMatcher createExactMatcher(String provideValue) {
        return new ExactMatcher(provideValue);
    }

    public static RequirementMatcher createVersionMatcher(String provideValue) {
        return new VersionMatcher(provideValue);
    }

    private static final class ExactMatcher implements RequirementMatcher {

        private String provides;

        private ExactMatcher(String provides) {
            this.provides = provides;
        }

        @Override
        public boolean matches(String requirement) {
            return provides.equalsIgnoreCase(requirement);
        }

        @Override
        public String toString() {
            return provides;
        }
    }

    private static final class VersionMatcher implements RequirementMatcher {
        DefaultArtifactVersion version;

        private VersionMatcher(String version) {
            this.version = new DefaultArtifactVersion(version);
        }

        @Override
        public boolean matches(String requirement) {
            try {
                VersionRange range = VersionRange.createFromVersionSpec(requirement);
                if (range.hasRestrictions()) {
                    return range.containsVersion(version);
                } else {
                    return range.getRecommendedVersion().compareTo(version) == 0;
                }
            } catch (InvalidVersionSpecificationException ex) {
                // TODO error reporting
                ex.printStackTrace();
                return false;
            }
        }

        @Override
        public String toString() {
            return version.toString();
        }
    }
}
