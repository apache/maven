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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.TempFileService;
import org.apache.maven.execution.MavenSession;

/**
 * Hooks into the Maven lifecycle and removes all temp material after the session.
 */
@Named
@Singleton
public final class TempFileCleanupParticipant extends AbstractMavenLifecycleParticipant {

    private final TempFileService tempFileService;

    @Inject
    public TempFileCleanupParticipant(final TempFileService tempFileService) {
        this.tempFileService = tempFileService;
    }

    @Override
    public void afterSessionEnd(final MavenSession mavenSession) {
        // Bridge to the API Session (available in Maven 4).
        final Session apiSession = mavenSession.getSession();
        try {
            tempFileService.cleanup(apiSession);
        } catch (final Exception e) {
            // We’re at session end; just log. Maven already reported build result.
            // Use slf4j directly to avoid throwing from the lifecycle callback.
            org.slf4j.LoggerFactory.getLogger(TempFileCleanupParticipant.class)
                    .warn("Temp cleanup failed: {}", e.getMessage());
        }
    }
}
