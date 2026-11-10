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
package org.apache.maven.exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Resolves the user-friendly help text associated with an {@link ExceptionSummary} reference.
 * <p>
 * Help text is stored as text resources in {@code org/apache/maven/errors/} of {@code maven-core}, one file per
 * reference key (for example {@code AetherClassNotFound.txt}). Unknown or third-party references for which no
 * resource exists resolve to {@code null} and produce no output.
 */
public final class ReferenceHelp {

    private static final String RESOURCE_PREFIX = "/org/apache/maven/errors/";
    private static final String CWIKI_PREFIX = "http://cwiki.apache.org/confluence/display/MAVEN/";
    private static final String RRF_URL = "https://maven.apache.org/resolver/remote-repository-filtering.html";

    private ReferenceHelp() {}

    /**
     * Returns the help text associated with the given exception reference (either a plain key such as
     * {@code AetherClassNotFound} or a classic wiki URL such as
     * {@code http://cwiki.apache.org/confluence/display/MAVEN/AetherClassNotFound}), or {@code null}
     * when no help text is available for this reference.
     *
     * @param reference the exception reference, may be {@code null} or empty
     * @return the help text, or {@code null} when none is available
     */
    public static String getHelpText(String reference) {
        String key = normalizeKey(reference);
        if (key == null) {
            return null;
        }
        try (InputStream in = ReferenceHelp.class.getResourceAsStream(RESOURCE_PREFIX + key + ".txt")) {
            if (in == null) {
                return null;
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // resources carry an Apache license header as '#' comments; strip it before returning
            StringBuilder text = new StringBuilder();
            boolean headerDone = false;
            for (String line : content.split("\r?\n", -1)) {
                if (!headerDone && line.startsWith("#")) {
                    continue;
                }
                headerDone = true;
                text.append(line).append('\n');
            }
            return text.toString().strip();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extracts the resource key from a reference (plain key or wiki URL) and validates it is safe to use
     * as a file name. Returns {@code null} when the reference cannot be mapped to a resource.
     */
    static String normalizeKey(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        String key = reference;
        if (key.startsWith(CWIKI_PREFIX)) {
            key = key.substring(CWIKI_PREFIX.length());
        } else if (RRF_URL.equals(key)) {
            key = "RemoteRepositoryFiltering";
        } else if (key.startsWith("http://") || key.startsWith("https://")) {
            int slash = key.lastIndexOf('/');
            if (slash < 0) {
                return null;
            }
            key = key.substring(slash + 1);
        } else if (key.indexOf('/') >= 0) {
            // a plain key must not be a path
            return null;
        }
        int query = key.indexOf('?');
        if (query >= 0) {
            key = key.substring(0, query);
        }
        if (key.isEmpty()) {
            return null;
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
                return null;
            }
        }
        return key;
    }
}
