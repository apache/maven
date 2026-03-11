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

import java.util.Locale;
import java.util.Objects;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionScheme;

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
        private final String provides;

        private ExactMatcher(String provides) {
            this.provides = provides;
        }

        @Override
        public boolean matches(String requirement) {
            return Objects.equals(
                    provides != null ? provides.toLowerCase(Locale.ENGLISH) : null,
                    requirement != null ? requirement.toLowerCase(Locale.ENGLISH) : null);
        }

        @Override
        public String toString() {
            return provides;
        }
    }

    private static final class VersionMatcher implements RequirementMatcher {
        private final String version;

        private VersionMatcher(String version) {
            this.version = version;
        }

        @Override
        public boolean matches(String requirement) {
            String r = requirement != null ? requirement.toLowerCase(Locale.ENGLISH) : null;
            String v = version != null ? version.toLowerCase(Locale.ENGLISH) : null;
            if (v == null && r == null) {
                return true; // null == null
            }
            if (v == null || r == null) {
                return false; // null != non-null
            }
            if (v.equals(r)) {
                return true; // str == str (ignoring case)
            }
            return matchesRequirement(v, r);
        }

        private static final VersionScheme VERSION_SCHEME = new GenericVersionScheme();

        private static boolean matchesRequirement(String version, String requirement) {
            // if requirement is not a version range itself
            if (!requirement.contains("[") && !requirement.contains("(") && !requirement.contains(",")) {
                boolean interval = false;
                boolean included = false;
                if (requirement.endsWith("+")) {
                    interval = true;
                    included = true;
                    requirement = requirement.substring(0, requirement.length() - 1);
                } else if (requirement.endsWith("-")) {
                    interval = true;
                    requirement = requirement.substring(0, requirement.length() - 1);
                }
                if (!interval) {
                    return version.startsWith(requirement + "."); // "11" -> "11.xxx"
                } else {
                    try {
                        if (included) {
                            return VERSION_SCHEME
                                    .parseVersionRange("[" + requirement + ",)")
                                    .containsVersion(VERSION_SCHEME.parseVersion(version)); // "11+" -> "[11,)"
                        } else {
                            return VERSION_SCHEME
                                    .parseVersionRange("(," + requirement + ")")
                                    .containsVersion(VERSION_SCHEME.parseVersion(version)); // "11-" -> "(,11)"
                        }
                    } catch (InvalidVersionSpecificationException e) {
                        // nope; GenericVersionScheme never throes but we need to make compiler happy
                        throw new RuntimeException(e);
                    }
                }
            } else {
                try {
                    return VERSION_SCHEME
                            .parseVersionRange(requirement)
                            .containsVersion(VERSION_SCHEME.parseVersion(version)); // "range" -> "range"
                } catch (InvalidVersionSpecificationException e) {
                    // nope; GenericVersionScheme never throes but we need to make compiler happy
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public String toString() {
            return version;
        }
    }
}
