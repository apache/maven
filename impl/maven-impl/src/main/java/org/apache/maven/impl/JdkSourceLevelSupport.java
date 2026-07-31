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
package org.apache.maven.impl;

/**
 * Utility class for JDK source level compatibility checks.
 * <p>
 * Maps JDK major versions to their supported {@code --source}/{@code --release} levels,
 * based on the javac retirement schedule defined in
 * <a href="https://openjdk.org/jeps/182">JEP 182</a> and subsequent JDK releases.
 * <p>
 * The retirement schedule follows these milestones:
 * <ul>
 *   <li>JDK 9: removed {@code --source 1} through {@code 5}, minimum is {@code 6}</li>
 *   <li>JDK 12: removed {@code --source 6}, minimum is {@code 7}</li>
 *   <li>JDK 21: removed {@code --source 7}, minimum is {@code 8}</li>
 * </ul>
 */
final class JdkSourceLevelSupport {

    private JdkSourceLevelSupport() {}

    /**
     * Returns the minimum {@code --source} level supported by a given JDK major version.
     *
     * @param jdkMajor the JDK major version (e.g., {@code 17}, {@code 21})
     * @return the minimum supported source level
     */
    static int minimumSupportedSourceLevel(int jdkMajor) {
        if (jdkMajor <= 8) {
            return 1;
        }
        if (jdkMajor <= 11) {
            return 6;
        }
        if (jdkMajor <= 20) {
            return 7;
        }
        return 8;
    }

    /**
     * Returns whether a given JDK version supports the specified {@code --source} level.
     *
     * @param jdkMajor    the JDK major version
     * @param sourceLevel the desired source level
     * @return {@code true} if the JDK supports the source level
     */
    static boolean supportsSourceLevel(int jdkMajor, int sourceLevel) {
        return sourceLevel >= minimumSupportedSourceLevel(jdkMajor) && sourceLevel <= jdkMajor;
    }

    /**
     * Normalizes a source level string to a major version number.
     * <p>
     * Handles legacy formats:
     * <ul>
     *   <li>{@code "1.5"} → {@code 5}</li>
     *   <li>{@code "1.8"} → {@code 8}</li>
     *   <li>{@code "11"} → {@code 11}</li>
     *   <li>{@code "21.0.1"} → {@code 21}</li>
     * </ul>
     *
     * @param version the source level string
     * @return the normalized major version, or {@code -1} if the string cannot be parsed
     */
    static int normalizeSourceLevel(String version) {
        if (version == null || version.isEmpty()) {
            return -1;
        }
        version = version.trim();
        // Handle "1.x" legacy format (e.g., "1.5", "1.8", "1.8.0_392")
        if (version.startsWith("1.") && version.length() > 2) {
            String rest = version.substring(2);
            // Strip any trailing qualifiers (e.g. "8.0_392" → "8")
            int sep = indexOfNonDigit(rest);
            if (sep > 0) {
                rest = rest.substring(0, sep);
            }
            try {
                return Integer.parseInt(rest);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        // Handle dotted versions like "21.0.1" — take the first segment
        int dotIndex = version.indexOf('.');
        if (dotIndex > 0) {
            version = version.substring(0, dotIndex);
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns the index of the first non-digit character in the string, or -1 if all characters are digits.
     */
    private static int indexOfNonDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the major version of the currently running JDK.
     *
     * @return the running JDK major version
     */
    static int getRunningJdkMajor() {
        return Runtime.version().feature();
    }
}
