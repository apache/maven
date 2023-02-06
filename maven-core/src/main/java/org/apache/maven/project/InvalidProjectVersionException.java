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
package org.apache.maven.project;

import java.io.File;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

/**
 * InvalidProjectVersionException
 */
public class InvalidProjectVersionException extends ProjectBuildingException {

    private final String locationInPom;

    private final String offendingVersion;

    public InvalidProjectVersionException(
            String projectId,
            String locationInPom,
            String offendingVersion,
            File pomFile,
            InvalidVersionSpecificationException cause) {
        super(projectId, formatMessage(projectId, locationInPom, offendingVersion, cause), pomFile, cause);
        this.locationInPom = locationInPom;
        this.offendingVersion = offendingVersion;
    }

    private static String formatMessage(
            String projectId,
            String locationInPom,
            String offendingVersion,
            InvalidVersionSpecificationException cause) {
        return "Invalid version: " + offendingVersion + " found for: " + locationInPom + " in project: " + projectId
                + ". Reason: " + cause.getMessage();
    }

    public String getOffendingVersion() {
        return offendingVersion;
    }

    public String getLocationInPom() {
        return locationInPom;
    }
}
