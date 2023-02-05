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
package org.apache.maven.model.resolution;

/**
 * Signals an error when resolving the path to an external model.
 *
 * @author Benjamin Bentmann
 */
public class UnresolvableModelException extends Exception {

    /**
     * The group id of the unresolvable model.
     */
    private final String groupId;

    /**
     * The artifact id of the unresolvable model.
     */
    private final String artifactId;

    /**
     * The version of the unresolvable model.
     */
    private final String version;

    /**
     * Creates a new exception with specified detail message and cause.
     *
     * @param message The detail message, may be {@code null}.
     * @param groupId The group id of the unresolvable model, may be {@code null}.
     * @param artifactId The artifact id of the unresolvable model, may be {@code null}.
     * @param version The version of the unresolvable model, may be {@code null}.
     * @param cause The cause, may be {@code null}.
     */
    public UnresolvableModelException(
            String message, String groupId, String artifactId, String version, Throwable cause) {
        super(message, cause);
        this.groupId = (groupId != null) ? groupId : "";
        this.artifactId = (artifactId != null) ? artifactId : "";
        this.version = (version != null) ? version : "";
    }

    /**
     * Creates a new exception with specified detail message.
     *
     * @param message The detail message, may be {@code null}.
     * @param groupId The group id of the unresolvable model, may be {@code null}.
     * @param artifactId The artifact id of the unresolvable model, may be {@code null}.
     * @param version The version of the unresolvable model, may be {@code null}.
     */
    public UnresolvableModelException(String message, String groupId, String artifactId, String version) {
        super(message);
        this.groupId = (groupId != null) ? groupId : "";
        this.artifactId = (artifactId != null) ? artifactId : "";
        this.version = (version != null) ? version : "";
    }

    /**
     * Creates a new exception with specified cause
     *
     * @param cause
     * @param groupId
     * @param artifactId
     * @param version
     */
    public UnresolvableModelException(Throwable cause, String groupId, String artifactId, String version) {
        super(cause);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Gets the group id of the unresolvable model.
     *
     * @return The group id of the unresolvable model, can be empty but never {@code null}.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the artifact id of the unresolvable model.
     *
     * @return The artifact id of the unresolvable model, can be empty but never {@code null}.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets the version of the unresolvable model.
     *
     * @return The version of the unresolvable model, can be empty but never {@code null}.
     */
    public String getVersion() {
        return version;
    }
}
