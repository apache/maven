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
package org.apache.maven;

import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultSession;
import org.eclipse.aether.DefaultRepositorySystemSession;

public class MavenTestHelper {
    public static DefaultRepositorySystemSession createSession(MavenRepositorySystem repositorySystem) {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenSession mavenSession = new MavenSession(repoSession, request, new DefaultMavenExecutionResult());
        DefaultSession session = new DefaultSession(mavenSession, null, null, repositorySystem, null, null);
        return repoSession;
    }
}
