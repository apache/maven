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
package org.apache.maven.artifact;

/**
 * Exception thrown when the identity of an artifact can not be established,
 * eg. one of groupId, artifactId, version or type is null.
 */
public class InvalidArtifactRTException extends RuntimeException {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String baseMessage;

    public InvalidArtifactRTException(String groupId, String artifactId, String version, String type, String message) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.baseMessage = message;
    }

    public InvalidArtifactRTException(
            String groupId, String artifactId, String version, String type, String message, Throwable cause) {
        super(cause);

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.baseMessage = message;
    }

    public String getMessage() {
        return "For artifact {" + getArtifactKey() + "}: " + getBaseMessage();
    }

    public String getBaseMessage() {
        return baseMessage;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactKey() {
        return groupId + ":" + artifactId + ":" + version + ":" + type;
    }
}
