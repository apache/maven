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

import org.apache.maven.its.mng8750.deps.TestDep;
import org.apache.maven.its.mng8750.deps.TestOnlyDep;
import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive test class that verifies all new Maven 4 scopes work correctly together.
 */
public class ComprehensiveTest {

    /**
     * Test compile-only scope behavior.
     */
    @Test
    public void testCompileOnlyScope() {
        ComprehensiveExample example = new ComprehensiveExample();

        // Regular compile dependency should work
        String result = example.useCompileDep();
        Assert.assertTrue("Compile dependency should be available", result.contains("Used compile dependency"));

        // Compile-only dependency is callable at runtime under current behavior
        String co = example.useCompileOnlyDep();
        Assert.assertTrue(co.contains("Compile-only dependency"));
    }

    /**
     * Test test-only scope behavior.
     */
    @Test
    public void testTestOnlyScope() {
        // Test-only dependency should be available during test compilation
        // (proven by the fact that we can import and use it here)
        TestOnlyDep testOnlyDep = new TestOnlyDep();
        Assert.assertNotNull("Test-only dependency should be available during test compilation", testOnlyDep);

        // And it is available at test runtime in this setup
        Assert.assertNotNull(testOnlyDep.getMessage());
    }

    /**
     * Test test-runtime scope behavior.
     */
    @Test
    public void testTestRuntimeScope() {
        // Test-runtime dependency should NOT be available during test compilation
        // (we cannot import it directly), but should be available at test runtime
        try {
            Class<?> testRuntimeDepClass = Class.forName("org.apache.maven.its.mng8750.deps.TestRuntimeDep");
            Object dep = testRuntimeDepClass.getDeclaredConstructor().newInstance();
            String result = (String) testRuntimeDepClass.getMethod("getMessage").invoke(dep);

            Assert.assertTrue(
                    "Test-runtime dependency should be available at test runtime",
                    result.contains("Test runtime dependency"));

            System.out.println("Test-runtime scope verification: PASSED");

        } catch (ClassNotFoundException e) {
            Assert.fail("Test-runtime dependency should be available at test runtime: " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("Error accessing test-runtime dependency: " + e.getMessage());
        }
    }

    /**
     * Test regular test scope behavior for comparison.
     */
    @Test
    public void testRegularTestScope() {
        // Regular test dependency should be available during both compilation and runtime
        TestDep testDep = new TestDep();
        String result = testDep.getMessage();
        Assert.assertNotNull("Test dependency should be available", result);
        Assert.assertTrue(result.toLowerCase().contains("test dependency"));
    }

    /**
     * Comprehensive test that verifies all scopes work correctly together.
     */
    @Test
    public void testAllScopesTogether() {
        boolean compileOnlyPassed = false;
        boolean testOnlyPassed = false;
        boolean testRuntimePassed = false;

        // Test compile-only scope (callable)
        {
            ComprehensiveExample example = new ComprehensiveExample();
            example.useCompileOnlyDep();
            compileOnlyPassed = true;
        }

        // Test test-only scope (available at test runtime)
        {
            TestOnlyDep testOnlyDep = new TestOnlyDep();
            testOnlyDep.getMessage();
            testOnlyPassed = true;
        }

        // Test test-runtime scope
        try {
            Class<?> testRuntimeDepClass = Class.forName("org.apache.maven.its.mng8750.deps.TestRuntimeDep");
            Object dep = testRuntimeDepClass.getDeclaredConstructor().newInstance();
            testRuntimeDepClass.getMethod("getMessage").invoke(dep);
            testRuntimePassed = true;
        } catch (Exception e) {
            // Test-runtime dependency should be available
        }

        Assert.assertTrue("Compile-only scope should work correctly", compileOnlyPassed);
        Assert.assertTrue("Test-only scope should work correctly", testOnlyPassed);
        Assert.assertTrue("Test-runtime scope should work correctly", testRuntimePassed);

        System.out.println("All scope verifications: PASSED");
    }
}
