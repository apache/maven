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
package org.apache.maven.internal;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Utility methods for checking test skip policy (e.g. {@code -Dmaven.test.skip}).
 */
public final class TestSkipUtils {

    private TestSkipUtils() {}

    /**
     * Determines whether test execution and compilation are skipped (e.g. via {@code -Dmaven.test.skip}).
     *
     * @param session The Maven session, may be {@code null}.
     * @param project The Maven project, may be {@code null}.
     * @return {@code true} if tests are skipped, {@code false} otherwise.
     */
    public static boolean isTestSkip(MavenSession session, MavenProject project) {
        if (session != null) {
            if (session.getUserProperties() != null) {
                String val = session.getUserProperties().getProperty("maven.test.skip");
                if (val != null) {
                    return Boolean.parseBoolean(val) || val.isEmpty();
                }
            }
            if (session.getSystemProperties() != null) {
                String val = session.getSystemProperties().getProperty("maven.test.skip");
                if (val != null) {
                    return Boolean.parseBoolean(val) || val.isEmpty();
                }
            }
            return isTestSkip(session.getRepositorySession(), project);
        }
        return isTestSkip((RepositorySystemSession) null, project);
    }

    /**
     * Determines whether test execution and compilation are skipped (e.g. via {@code -Dmaven.test.skip}).
     *
     * @param session The repository system session, may be {@code null}.
     * @param project The Maven project, may be {@code null}.
     * @return {@code true} if tests are skipped, {@code false} otherwise.
     */
    public static boolean isTestSkip(RepositorySystemSession session, MavenProject project) {
        if (session != null) {
            Object val = session.getConfigProperties().get("maven.test.skip");
            if (val != null) {
                String s = String.valueOf(val);
                return Boolean.parseBoolean(s) || s.isEmpty();
            }
            val = session.getUserProperties().get("maven.test.skip");
            if (val != null) {
                String s = String.valueOf(val);
                return Boolean.parseBoolean(s) || s.isEmpty();
            }
            val = session.getSystemProperties().get("maven.test.skip");
            if (val != null) {
                String s = String.valueOf(val);
                return Boolean.parseBoolean(s) || s.isEmpty();
            }
        }
        if (project != null && project.getProperties() != null) {
            String val = project.getProperties().getProperty("maven.test.skip");
            if (val != null) {
                return Boolean.parseBoolean(val) || val.isEmpty();
            }
        }
        return false;
    }
}
