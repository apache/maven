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
package org.apache.maven.plugin.testing;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;

public class PluginArtifactFileTest extends AbstractMojoTestCase {
    private static final String FS = System.getProperty("file.separator");

    public void testArtifact() throws Exception {
        MojoExecution execution = newMojoExecution("parameters"); // TODO dedicated test mojo

        List<Artifact> artifacts =
                execution.getMojoDescriptor().getPluginDescriptor().getArtifacts();

        assertEquals(1, artifacts.size());

        Artifact artifact = artifacts.get(0);
        assertEquals("test", artifact.getGroupId());
        assertEquals("test-plugin", artifact.getArtifactId());
        assertEquals("0.0.1-SNAPSHOT", artifact.getBaseVersion());
        assertTrue(artifact.getFile().getAbsolutePath().endsWith(FS + "target" + FS + "test-classes"));
    }

    // TODO find a way to automate testing of jar:file:/ test plugin URLs
}
