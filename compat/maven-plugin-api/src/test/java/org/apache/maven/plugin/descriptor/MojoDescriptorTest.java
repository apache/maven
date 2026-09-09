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
package org.apache.maven.plugin.descriptor;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojoDescriptorTest {

    @Test
    void idBasedCycleDetectionWithClonedDescriptors() {
        // Simulate two independently-loaded PluginDescriptor instances for the same plugin,
        // as happens when DefaultPluginDescriptorCache clones on every cache retrieval.
        PluginDescriptor pd1 = new PluginDescriptor();
        pd1.setGroupId("org.example");
        pd1.setArtifactId("example-plugin");
        pd1.setVersion("1.0.0");

        PluginDescriptor pd2 = new PluginDescriptor();
        pd2.setGroupId("org.example");
        pd2.setArtifactId("example-plugin");
        pd2.setVersion("1.0.0");

        assertNotSame(pd1, pd2, "must be different object instances");

        MojoDescriptor md1 = new MojoDescriptor();
        md1.setGoal("compile");
        md1.setPluginDescriptor(pd1);

        MojoDescriptor md2 = new MojoDescriptor();
        md2.setGoal("compile");
        md2.setPluginDescriptor(pd2);

        assertNotSame(md1, md2, "must be different object instances");

        // getId() must produce the same key for both independently-loaded descriptors
        assertEquals(md1.getId(), md2.getId(), "IDs must match for same groupId:artifactId:version:goal");

        // String-based cycle detection set must detect the duplicate
        Set<String> alreadyPlanned = new HashSet<>();
        alreadyPlanned.add(md1.getId());
        assertTrue(alreadyPlanned.contains(md2.getId()), "cycle detection must find the cloned descriptor by ID");
    }

    @Test
    void getParameterMap() throws DuplicateParameterException {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        Parameter param1 = new Parameter();
        param1.setName("param1");
        param1.setDefaultValue("value1");
        mojoDescriptor.addParameter(param1);

        assertEquals(1, mojoDescriptor.getParameters().size());

        assertEquals(
                mojoDescriptor.getParameters().size(),
                mojoDescriptor.getParameterMap().size());

        Parameter param2 = new Parameter();
        param2.setName("param2");
        param2.setDefaultValue("value2");
        mojoDescriptor.addParameter(param2);

        assertEquals(2, mojoDescriptor.getParameters().size());
        assertEquals(
                mojoDescriptor.getParameters().size(),
                mojoDescriptor.getParameterMap().size());
    }
}
