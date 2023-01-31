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
package org.apache.maven.artifact.resolver.filter;

import java.util.Arrays;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;

/**
 * Tests {@link OrArtifactFilter}.
 *
 * @author Benjamin Bentmann
 */
public class OrArtifactFilterTest extends TestCase {

    private ArtifactFilter newSubFilter() {
        return new ArtifactFilter() {
            public boolean include(Artifact artifact) {
                return false;
            }
        };
    }

    public void testEquals() {
        OrArtifactFilter filter1 = new OrArtifactFilter();

        OrArtifactFilter filter2 = new OrArtifactFilter(Arrays.asList(newSubFilter()));

        assertFalse(filter1.equals(null));
        assertTrue(filter1.equals(filter1));
        assertEquals(filter1.hashCode(), filter1.hashCode());

        assertFalse(filter1.equals(filter2));
        assertFalse(filter2.equals(filter1));
    }
}
