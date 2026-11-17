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
package org.apache.maven.its.mng8750;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class to verify compile-only scope behavior.
 */
public class CompileOnlyTest {

    /**
     * Test that regular compile dependencies are available at runtime.
     */
    @Test
    public void testCompileDependencyAvailableAtRuntime() {
        CompileOnlyExample example = new CompileOnlyExample();
        String result = example.useCompileDep();
        Assertions.assertTrue(result.contains("Used compile dependency"), "Compile dependency should be available");
    }

    /**
     * Test that compile-only dependencies are callable at runtime (current behavior under test).
     */
    @Test
    public void testCompileOnlyDependencyCallableAtRuntime() {
        CompileOnlyExample example = new CompileOnlyExample();
        String result = example.useCompileOnlyDep();
        Assertions.assertTrue(result.contains("Compile-only dependency"), "Compile-only dependency should be callable");
    }

    /**
     * Test that verifies the main method behavior.
     */
    @Test
    public void testMainMethodBehavior() {
        // This test ensures that the main method runs without throwing unexpected exceptions
        // The main method itself handles the NoClassDefFoundError for compile-only dependencies
        try {
            CompileOnlyExample.main(new String[0]);
        } catch (Exception e) {
            Assertions.fail("Main method should handle compile-only dependency gracefully: " + e.getMessage());
        }
    }
}
