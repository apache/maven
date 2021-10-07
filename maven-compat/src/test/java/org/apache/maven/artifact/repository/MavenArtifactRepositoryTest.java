package org.apache.maven.artifact.repository;

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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenArtifactRepositoryTest
{
    private static class MavenArtifactRepositorySubclass extends MavenArtifactRepository
    {
        String id;

        public MavenArtifactRepositorySubclass(String id)
        {
            this.id = id;
        }

        @Override
        public String getId()
        {
            return id;
        }
    }

    @Test
    public void testHashCodeEquals()
    {
        MavenArtifactRepositorySubclass r1 = new MavenArtifactRepositorySubclass( "foo" );
        MavenArtifactRepositorySubclass r2 = new MavenArtifactRepositorySubclass( "foo" );
        MavenArtifactRepositorySubclass r3 = new MavenArtifactRepositorySubclass( "bar" );

        assertEquals( r1.hashCode(), r2.hashCode() );
        assertNotEquals( r1.hashCode(), r3.hashCode() );

        assertEquals( r1, r2 );
        assertEquals( r2, r1 );

        assertNotEquals( r1, r3 );
        assertNotEquals( r3, r1 );
    }
}
