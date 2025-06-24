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
package org.apache.maven.impl.model;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectPool;
import org.apache.maven.api.model.ModelObjectPoolFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DefaultModelObjectPool.
 */
class DefaultModelObjectPoolTest {

    @Test
    void testServiceLoading() {
        ModelObjectPool pool = ModelObjectPoolFactory.getInstance();
        assertNotNull(pool);
        assertTrue(pool instanceof DefaultModelObjectPool);
    }

    @Test
    void testDependencyPooling() {
        ModelObjectPool pool = new DefaultModelObjectPool();
        
        // Create two identical dependencies
        Dependency dep1 = Dependency.newBuilder()
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .version("4.0.0")
                .build();
        
        Dependency dep2 = Dependency.newBuilder()
                .groupId("org.apache.maven")
                .artifactId("maven-core")
                .version("4.0.0")
                .build();
        
        // They should be different instances initially
        assertNotSame(dep1, dep2);
        assertEquals(dep1, dep2);
        
        // After interning, they should be the same instance
        Dependency pooled1 = pool.intern(dep1);
        Dependency pooled2 = pool.intern(dep2);
        
        assertSame(pooled1, pooled2);
        assertEquals(dep1, pooled1);
        assertEquals(dep2, pooled2);
    }

    @Test
    void testNonDependencyObjects() {
        ModelObjectPool pool = new DefaultModelObjectPool();
        
        String testString = "test";
        String result = pool.intern(testString);
        
        // Non-dependency objects should be returned as-is
        assertSame(testString, result);
    }

    @Test
    void testStatistics() {
        DefaultModelObjectPool pool = new DefaultModelObjectPool();
        ModelObjectPool.PoolStatistics stats = pool.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.getPoolSize());
        assertEquals(0, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(0.0, stats.getHitRatio());
        
        // Create and intern a dependency
        Dependency dep = Dependency.newBuilder()
                .groupId("test")
                .artifactId("test")
                .version("1.0")
                .build();
        
        pool.intern(dep);
        
        stats = pool.getStatistics();
        assertEquals(1, stats.getPoolSize());
        assertEquals(0, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(0.0, stats.getHitRatio());
        
        // Intern the same dependency again
        pool.intern(dep);
        
        stats = pool.getStatistics();
        assertEquals(1, stats.getPoolSize());
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(0.5, stats.getHitRatio());
    }

    @Test
    void testSupports() {
        DefaultModelObjectPool pool = new DefaultModelObjectPool();
        
        assertTrue(pool.supports(Dependency.class));
        assertFalse(pool.supports(String.class));
    }

    @Test
    void testPriority() {
        DefaultModelObjectPool pool = new DefaultModelObjectPool();
        assertEquals(100, pool.getPriority());
    }
}
