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

import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultSessionTest {

    @Test
    void testRootDirectoryWithNull() {
        RepositorySystemSession rss = MavenRepositorySystemUtils.newSession();
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        DefaultSession session =
                new DefaultSession(ms, new DefaultRepositorySystem(), Collections.emptyList(), null, null, null);

        assertEquals(
                RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE,
                assertThrows(IllegalStateException.class, session::getRootDirectory)
                        .getMessage());
    }

    @Test
    void testRootDirectory() {
        RepositorySystemSession rss = MavenRepositorySystemUtils.newSession();
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        ms.getRequest().setRootDirectory(Paths.get("myRootDirectory"));
        DefaultSession session =
                new DefaultSession(ms, new DefaultRepositorySystem(), Collections.emptyList(), null, null, null);

        assertEquals(Paths.get("myRootDirectory"), session.getRootDirectory());
    }
}
