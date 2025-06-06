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
package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.logging.Logger;

/**
 * Send resolution warning events to the warning log.
 *
 */
@Deprecated
public class WarningResolutionListener implements ResolutionListener {
    private Logger logger;

    public WarningResolutionListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void testArtifact(Artifact node) {}

    @Override
    public void startProcessChildren(Artifact artifact) {}

    @Override
    public void endProcessChildren(Artifact artifact) {}

    @Override
    public void includeArtifact(Artifact artifact) {}

    @Override
    public void omitForNearer(Artifact omitted, Artifact kept) {}

    @Override
    public void omitForCycle(Artifact omitted) {}

    @Override
    public void updateScopeCurrentPom(Artifact artifact, String scope) {}

    @Override
    public void updateScope(Artifact artifact, String scope) {}

    @Override
    public void manageArtifact(Artifact artifact, Artifact replacement) {}

    @Override
    public void selectVersionFromRange(Artifact artifact) {}

    @Override
    public void restrictRange(Artifact artifact, Artifact replacement, VersionRange newRange) {}
}
