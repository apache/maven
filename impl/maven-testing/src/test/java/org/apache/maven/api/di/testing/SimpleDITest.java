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
package org.apache.maven.api.di.testing;

import java.io.File;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.stubs.SessionStub;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.di.testing.MavenDIExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MavenDITest
public class SimpleDITest {

    private static final String LOCAL_REPO = getBasedir() + File.separator + "target" + File.separator + "local-repo";

    @Inject
    Session session;

    @Test
    void testSession() {
        assertNotNull(session);
        assertNotNull(session.getLocalRepository());
    }

    @Provides
    Session createSession() {
        return SessionStub.getMockSession(LOCAL_REPO);
    }
}
