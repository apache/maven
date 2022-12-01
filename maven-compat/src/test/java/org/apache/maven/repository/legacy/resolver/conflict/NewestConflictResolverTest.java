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
package org.apache.maven.repository.legacy.resolver.conflict;

import org.apache.maven.artifact.resolver.ResolutionNode;

/**
 * Tests <code>NewestConflictResolver</code>.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @see NewestConflictResolver
 */
public class NewestConflictResolverTest extends AbstractConflictResolverTest {
    // constructors -----------------------------------------------------------

    public NewestConflictResolverTest() throws Exception {
        super("newest");
    }

    // tests ------------------------------------------------------------------

    /**
     * Tests that <code>a:2.0</code> wins in the scenario:
     * <pre>
     * a:1.0
     * b:1.0 -&gt; a:2.0
     * </pre>
     */
    public void testDepth() {
        ResolutionNode a1n = createResolutionNode(a1);
        ResolutionNode b1n = createResolutionNode(b1);
        ResolutionNode a2n = createResolutionNode(a2, b1n);

        assertResolveConflict(a2n, a1n, a2n);
    }

    /**
     * Tests that <code>a:2.0</code> wins in the scenario:
     * <pre>
     * b:1.0 -&gt; a:2.0
     * a:1.0
     * </pre>
     */
    public void testDepthReversed() {
        ResolutionNode b1n = createResolutionNode(b1);
        ResolutionNode a2n = createResolutionNode(a2, b1n);
        ResolutionNode a1n = createResolutionNode(a1);

        assertResolveConflict(a2n, a2n, a1n);
    }

    /**
     * Tests that <code>a:2.0</code> wins in the scenario:
     * <pre>
     * a:1.0
     * a:2.0
     * </pre>
     */
    public void testEqual() {
        ResolutionNode a1n = createResolutionNode(a1);
        ResolutionNode a2n = createResolutionNode(a2);

        assertResolveConflict(a2n, a1n, a2n);
    }

    /**
     * Tests that <code>a:2.0</code> wins in the scenario:
     * <pre>
     * a:2.0
     * a:1.0
     * </pre>
     */
    public void testEqualReversed() {
        ResolutionNode a2n = createResolutionNode(a2);
        ResolutionNode a1n = createResolutionNode(a1);

        assertResolveConflict(a2n, a2n, a1n);
    }
}
