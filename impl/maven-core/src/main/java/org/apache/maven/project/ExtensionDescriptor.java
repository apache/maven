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

import java.util.ArrayList;
import java.util.List;

/**
 * Provides metadata about a build extension. <strong>Warning:</strong> This is an internal utility class that is only
 * public for technical reasons, it is not part of the public API. In particular, this class can be changed or deleted
 * without prior notice.
 *
 */
public class ExtensionDescriptor {

    private List<String> exportedPackages;

    private List<String> exportedArtifacts;

    ExtensionDescriptor() {
        // hide constructor
    }

    public List<String> getExportedPackages() {
        if (exportedPackages == null) {
            exportedPackages = new ArrayList<>();
        }

        return exportedPackages;
    }

    public void setExportedPackages(List<String> exportedPackages) {
        if (exportedPackages == null) {
            this.exportedPackages = null;
        } else {
            this.exportedPackages = new ArrayList<>(exportedPackages);
        }
    }

    public List<String> getExportedArtifacts() {
        if (exportedArtifacts == null) {
            exportedArtifacts = new ArrayList<>();
        }

        return exportedArtifacts;
    }

    public void setExportedArtifacts(List<String> exportedArtifacts) {
        if (exportedArtifacts == null) {
            this.exportedArtifacts = null;
        } else {
            this.exportedArtifacts = new ArrayList<>(exportedArtifacts);
        }
    }
}
