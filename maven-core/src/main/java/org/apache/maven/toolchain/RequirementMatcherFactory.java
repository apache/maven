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

import java.util.regex.Pattern;

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
                VersionRange range = convertRequirementToVersionRange(requirement);
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

        private VersionRange convertRequirementToVersionRange(String requirement)
                throws InvalidVersionSpecificationException {
            // Specific for Version _requirement_ matching;
            // If the version is a simple integer (like "25")
            // then treat this as the requirement "the major version is 25"
            if (Pattern.matches("^[0-9]+$", requirement)) {
                int majorVersion = Integer.parseInt(requirement);
                return VersionRange.createFromVersionSpec("[" + majorVersion + "," + (majorVersion + 1) + ")");
            }

            // If the version is a major.minor (like "1.5")
            // then treat this as the requirement "the major version is 1 and the minor is 5"
            if (Pattern.matches("^[0-9]\\.[0-9]+$", requirement)) {
                String[] split = requirement.split("\\.", 2);
                int majorVersion = Integer.parseInt(split[0]);
                int minorVersion = Integer.parseInt(split[1]);
                return VersionRange.createFromVersionSpec(
                        "[" + majorVersion + "." + minorVersion + "," + majorVersion + "." + (minorVersion + 1) + ")");
            }

            return VersionRange.createFromVersionSpec(requirement);
        }

        @Override
        public String toString() {
            return version.toString();
        }
    }
}
