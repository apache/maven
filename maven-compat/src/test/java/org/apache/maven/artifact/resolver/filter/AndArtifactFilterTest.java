package org.apache.maven.artifact.resolver.filter;

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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link AndArtifactFilter}.
 *
 * @author Benjamin Bentmann
 */
public class AndArtifactFilterTest
{

    private ArtifactFilter newSubFilter()
    {
        return artifact -> false;
    }

    @Test
    public void testEquals()
    {
        AndArtifactFilter filter1 = new AndArtifactFilter();

        AndArtifactFilter filter2 = new AndArtifactFilter( Arrays.asList( newSubFilter() ) );

        assertFalse( filter1.equals( null ) );
        assertTrue( filter1.equals( filter1 ) );
        assertEquals( filter1.hashCode(), filter1.hashCode() );

        assertFalse( filter1.equals( filter2 ) );
        assertFalse( filter2.equals( filter1 ) );
    }

}
