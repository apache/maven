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
package org.apache.maven.repository.metadata;

import org.codehaus.plexus.PlexusTestCase;

/**
 *
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public class DefaultGraphConflictResolutionPolicyTest extends PlexusTestCase {
    GraphConflictResolutionPolicy policy;
    MetadataGraphEdge e1;
    MetadataGraphEdge e2;
    MetadataGraphEdge e3;
    // ------------------------------------------------------------------------------------------
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        policy = (GraphConflictResolutionPolicy) lookup(GraphConflictResolutionPolicy.ROLE, "default");
        e1 = new MetadataGraphEdge("1.1", true, null, null, 2, 1);
        e2 = new MetadataGraphEdge("1.2", true, null, null, 3, 2);
        e3 = new MetadataGraphEdge("1.2", true, null, null, 2, 3);
    }
    // ------------------------------------------------------------------------------------------
    public void testDefaultPolicy() throws Exception {
        MetadataGraphEdge res;

        res = policy.apply(e1, e2);
        assertEquals("Wrong depth edge selected", "1.1", res.getVersion());

        res = policy.apply(e1, e3);
        assertEquals("Wrong version edge selected", "1.2", res.getVersion());
    }
    // ------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------
}
