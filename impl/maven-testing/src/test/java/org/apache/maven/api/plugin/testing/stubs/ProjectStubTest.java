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
package org.apache.maven.api.plugin.testing.stubs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectStubTest {

    @Test
    @SuppressWarnings("removal")
    void chainedSettersKeepTheDeprecatedType() {
        ProjectStub project = new ProjectStub()
                .setGroupId("group")
                .setArtifactId("artifact")
                .setVersion("1.0")
                .setPackaging("jar")
                .addProperty("key", "value");

        assertEquals("group", project.getGroupId());
        assertEquals("artifact", project.getArtifactId());
        assertEquals("1.0", project.getVersion());
        assertEquals("jar", project.getPackaging().id());
        assertEquals("value", project.getModel().getProperties().get("key"));
    }
}
