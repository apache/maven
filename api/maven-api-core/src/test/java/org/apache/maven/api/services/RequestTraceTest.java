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
package org.apache.maven.api.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RequestTraceTest {

    @Test
    void requestTraceCreation() {
        RequestTrace parentTrace = new RequestTrace("parent-context", null, "parent-data");
        RequestTrace childTrace = new RequestTrace("child-context", parentTrace, "child-data");

        assertEquals("parent-context", parentTrace.context());
        assertNull(parentTrace.parent());
        assertEquals("parent-data", parentTrace.data());

        assertEquals("child-context", childTrace.context());
        assertSame(parentTrace, childTrace.parent());
        assertEquals("child-data", childTrace.data());
    }

    @Test
    void requestTraceWithParentContextInheritance() {
        RequestTrace parentTrace = new RequestTrace("parent-context", null, "parent-data");
        RequestTrace childTrace = new RequestTrace(parentTrace, "child-data");

        assertEquals("parent-context", parentTrace.context());
        assertEquals("parent-context", childTrace.context());
        assertEquals("child-data", childTrace.data());
    }

    @Test
    void predefinedContexts() {
        assertEquals("plugin", RequestTrace.CONTEXT_PLUGIN);
        assertEquals("project", RequestTrace.CONTEXT_PROJECT);
        assertEquals("bootstrap", RequestTrace.CONTEXT_BOOTSTRAP);
    }

    @Test
    void nullValues() {
        RequestTrace trace = new RequestTrace(null, null, null);
        assertNull(trace.context());
        assertNull(trace.parent());
        assertNull(trace.data());
    }

    @Test
    void chainedTraces() {
        RequestTrace root = new RequestTrace("root", null, "root-data");
        RequestTrace level1 = new RequestTrace("level1", root, "level1-data");
        RequestTrace level2 = new RequestTrace("level2", level1, "level2-data");
        RequestTrace level3 = new RequestTrace(level2, "level3-data");

        // Verify the chain
        assertNull(root.parent());
        assertEquals(root, level1.parent());
        assertEquals(level1, level2.parent());
        assertEquals(level2, level3.parent());

        // Verify context inheritance
        assertEquals("root", root.context());
        assertEquals("level1", level1.context());
        assertEquals("level2", level2.context());
        assertEquals("level2", level3.context()); // Inherited from parent
    }
}
