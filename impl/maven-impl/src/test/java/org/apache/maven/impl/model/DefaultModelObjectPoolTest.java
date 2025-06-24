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
import org.apache.maven.api.model.ModelObjectProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DefaultModelObjectPool.
 */
class DefaultModelObjectPoolTest {

    @Test
    void testServiceLoading() {
        // Test that the static method works
        String testString = "test";
        String result = ModelObjectProcessor.processObject(testString);
        assertNotNull(result);
        assertEquals(testString, result);
    }

    @Test
    void testDependencyPooling() {
        ModelObjectProcessor processor = new DefaultModelObjectPool();

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

        // After processing, they should be the same instance
        Dependency pooled1 = processor.process(dep1);
        Dependency pooled2 = processor.process(dep2);

        assertSame(pooled1, pooled2);
        assertEquals(dep1, pooled1);
        assertEquals(dep2, pooled2);
    }

    @Test
    void testNonDependencyObjects() {
        ModelObjectProcessor processor = new DefaultModelObjectPool();

        String testString = "test";
        String result = processor.process(testString);

        // Non-dependency objects should be returned as-is
        assertSame(testString, result);
    }


}
