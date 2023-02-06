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
package org.apache.maven.model.building;

import java.io.File;

import org.apache.maven.model.Model;

/**
 * Assists in the handling of model problems.
 *
 * @author Benjamin Bentmann
 */
public class ModelProblemUtils {

    /**
     * Creates a user-friendly source hint for the specified model.
     *
     * @param model The model to create a source hint for, may be {@code null}.
     * @return The user-friendly source hint, never {@code null}.
     */
    static String toSourceHint(Model model) {
        if (model == null) {
            return "";
        }

        StringBuilder buffer = new StringBuilder(128);

        buffer.append(toId(model));

        File pomFile = model.getPomFile();
        if (pomFile != null) {
            buffer.append(" (").append(pomFile).append(')');
        }

        return buffer.toString();
    }

    static String toPath(Model model) {
        String path = "";

        if (model != null) {
            File pomFile = model.getPomFile();

            if (pomFile != null) {
                path = pomFile.getAbsolutePath();
            }
        }

        return path;
    }

    static String toId(Model model) {
        if (model == null) {
            return "";
        }

        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }

        String artifactId = model.getArtifactId();

        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        if (version == null) {
            version = "[unknown-version]";
        }

        return toId(groupId, artifactId, version);
    }

    /**
     * Creates a user-friendly artifact id from the specified coordinates.
     *
     * @param groupId The group id, may be {@code null}.
     * @param artifactId The artifact id, may be {@code null}.
     * @param version The version, may be {@code null}.
     * @return The user-friendly artifact id, never {@code null}.
     */
    static String toId(String groupId, String artifactId, String version) {
        StringBuilder buffer = new StringBuilder(128);

        buffer.append((groupId != null && groupId.length() > 0) ? groupId : "[unknown-group-id]");
        buffer.append(':');
        buffer.append((artifactId != null && artifactId.length() > 0) ? artifactId : "[unknown-artifact-id]");
        buffer.append(':');
        buffer.append((version != null && version.length() > 0) ? version : "[unknown-version]");

        return buffer.toString();
    }

    /**
     * Creates a string with all location details for the specified model problem. If the project identifier is
     * provided, the generated location will omit the model id and source information and only give line/column
     * information for problems originating directly from this POM.
     *
     * @param problem The problem whose location should be formatted, must not be {@code null}.
     * @param projectId The {@code <groupId>:<artifactId>:<version>} of the corresponding project, may be {@code null}
     *            to force output of model id and source.
     * @return The formatted problem location or an empty string if unknown, never {@code null}.
     */
    public static String formatLocation(ModelProblem problem, String projectId) {
        StringBuilder buffer = new StringBuilder(256);

        if (!problem.getModelId().equals(projectId)) {
            buffer.append(problem.getModelId());

            if (problem.getSource().length() > 0) {
                if (buffer.length() > 0) {
                    buffer.append(", ");
                }
                buffer.append(problem.getSource());
            }
        }

        if (problem.getLineNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("line ").append(problem.getLineNumber());
        }

        if (problem.getColumnNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("column ").append(problem.getColumnNumber());
        }

        return buffer.toString();
    }
}
