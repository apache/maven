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
package org.apache.maven.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class CacheManagerTest {

    @Test
    void testSingletonInstance() {
        CacheManager cm1 = CacheManager.getInstance();
        CacheManager cm2 = CacheManager.getInstance();

        assertSame(cm1, cm2); // Should return same instance
    }

    @Test
    void testCachingOfInputLocation() {
        InputLocation loc1 = InputLocation.location(1, 1);
        InputLocation loc2 = InputLocation.location(1, 1);

        // Cached instances should be the same object
        assertSame(loc1, loc2);
    }

    @Test
    void testCachingOfInputSource() {
        InputSource src1 = InputSource.source("g:a:1.0", "pom.xml");
        InputSource src2 = InputSource.source("g:a:1.0", "pom.xml");

        // Cached instances should be the same object
        assertSame(src1, src2);
    }

    @Test
    void testCachingOfDependency() {
        Dependency dep1 = Dependency.newBuilder()
                .groupId(new String("myGroup"))
                .artifactId(new String("myArtifact"))
                .build();
        Dependency dep2 = Dependency.newBuilder()
                .groupId(new String("myGroup"))
                .artifactId(new String("myArtifact"))
                .build();

        // Cached instances should be the same object
        assertSame(dep1, dep2);
    }
}
