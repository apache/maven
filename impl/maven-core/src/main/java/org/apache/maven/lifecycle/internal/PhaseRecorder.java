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
package org.apache.maven.lifecycle.internal;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 * @since 3.0
 */
public class PhaseRecorder {
    private String lastLifecyclePhase;

    private final MavenProject project;

    public PhaseRecorder(MavenProject project) {
        this.project = project;
    }

    public void observeExecution(MojoExecution mojoExecution) {
        String lifecyclePhase = mojoExecution.getLifecyclePhase();

        if (lifecyclePhase != null) {
            PhaseId phaseId = PhaseId.of(lifecyclePhase);
            if (lastLifecyclePhase == null) {
                lastLifecyclePhase = phaseId.phase();
            } else if (!phaseId.phase().equals(lastLifecyclePhase)) {
                project.addLifecyclePhase(lastLifecyclePhase);
                lastLifecyclePhase = phaseId.phase();
            }
        }

        if (lastLifecyclePhase != null) {
            project.addLifecyclePhase(lastLifecyclePhase);
        }
    }

    public boolean isDifferentPhase(MojoExecution nextMojoExecution) {
        String lifecyclePhase = nextMojoExecution.getLifecyclePhase();
        if (lifecyclePhase == null) {
            return lastLifecyclePhase != null;
        }
        return !PhaseId.of(lifecyclePhase).phase().equals(lastLifecyclePhase);
    }
}
