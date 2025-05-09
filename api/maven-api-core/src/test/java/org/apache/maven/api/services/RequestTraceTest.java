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

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceTest {

    @Test
    void requestTraceCreation() {
        RequestTrace parentTrace = new RequestTrace("parent-context", null, "parent-data");
        RequestTrace childTrace = new RequestTrace("child-context", parentTrace, "child-data");

        assertThat(parentTrace.context()).isEqualTo("parent-context");
        assertThat(parentTrace.parent()).isNull();
        assertThat(parentTrace.data()).isEqualTo("parent-data");

        assertThat(childTrace.context()).isEqualTo("child-context");
        assertThat(childTrace.parent()).isSameAs(parentTrace);
        assertThat(childTrace.data()).isEqualTo("child-data");
    }

    @Test
    void requestTraceWithParentContextInheritance() {
        RequestTrace parentTrace = new RequestTrace("parent-context", null, "parent-data");
        RequestTrace childTrace = new RequestTrace(parentTrace, "child-data");

        assertThat(parentTrace.context()).isEqualTo("parent-context");
        assertThat(childTrace.context()).isEqualTo("parent-context");
        assertThat(childTrace.data()).isEqualTo("child-data");
    }

    @Test
    void predefinedContexts() {
        assertThat(RequestTrace.CONTEXT_PLUGIN).isEqualTo("plugin");
        assertThat(RequestTrace.CONTEXT_PROJECT).isEqualTo("project");
        assertThat(RequestTrace.CONTEXT_BOOTSTRAP).isEqualTo("bootstrap");
    }

    @Test
    void nullValues() {
        RequestTrace trace = new RequestTrace(null, null, null);
        assertThat(trace.context()).isNull();
        assertThat(trace.parent()).isNull();
        assertThat(trace.data()).isNull();
    }

    @Test
    void chainedTraces() {
        RequestTrace root = new RequestTrace("root", null, "root-data");
        RequestTrace level1 = new RequestTrace("level1", root, "level1-data");
        RequestTrace level2 = new RequestTrace("level2", level1, "level2-data");
        RequestTrace level3 = new RequestTrace(level2, "level3-data");

        // Verify the chain
        assertThat(root.parent()).isNull();
        assertThat(level1.parent()).isEqualTo(root);
        assertThat(level2.parent()).isEqualTo(level1);
        assertThat(level3.parent()).isEqualTo(level2);

        // Verify context inheritance
        assertThat(root.context()).isEqualTo("root");
        assertThat(level1.context()).isEqualTo("level1");
        assertThat(level2.context()).isEqualTo("level2");
        assertThat(level3.context()).isEqualTo("level2"); // Inherited from parent
    }
}
