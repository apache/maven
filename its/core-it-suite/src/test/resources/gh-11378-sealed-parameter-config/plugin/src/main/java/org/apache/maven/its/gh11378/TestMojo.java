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
package org.apache.maven.its.gh11378;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

@Mojo(name = "test-goal")
public class TestMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private Log log;

    @Parameter
    private Artifact artifact;

    @Override
    public void execute() {
        if (!(artifact instanceof LocalArtifact)) {
            throw new IllegalStateException("Expected LocalArtifact but got " + artifact);
        }
        LocalArtifact localArtifact = (LocalArtifact) artifact;
        if (!"local".equals(localArtifact.name)) {
            throw new IllegalStateException("Expected artifact name 'local' but got '" + localArtifact.name + "'");
        }
        log.info("Configured sealed artifact: " + localArtifact.name);
    }

    public sealed interface Artifact permits LocalArtifact, RemoteArtifact {}

    public static final class LocalArtifact implements Artifact {

        public String name;
    }

    public static final class RemoteArtifact implements Artifact {

        public String url;
    }
}
