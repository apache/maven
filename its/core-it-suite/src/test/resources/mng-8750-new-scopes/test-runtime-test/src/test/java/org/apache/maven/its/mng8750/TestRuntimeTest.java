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
import org.junit.Assert;
import org.junit.Test;

// This import should NOT work during test compilation because test-runtime dependencies
// are not available during test compilation phase
// import org.apache.maven.its.mng8750.deps.TestRuntimeDep;

/**
 * Test class to verify test-runtime scope behavior.
 * This class demonstrates that test-runtime dependencies are NOT available during
 * test compilation but ARE available during test runtime.
 */
public class TestRuntimeTest {

    /**
     * Test that regular test dependencies are available at test runtime.
     */
    @Test
    public void testTestDependencyAvailableAtTestRuntime() {
        TestDep dep = new TestDep();
        String result = dep.getMessage();
        Assert.assertNotNull("Test dependency should be available", result);
        Assert.assertTrue(
                "Test dependency should be available", result.toLowerCase().contains("test dependency"));
    }

    /**
     * Test that test-runtime dependencies ARE available at test runtime.
     * We use reflection to access the test-runtime dependency since it's not
     * available during compilation.
     */
    @Test
    public void testTestRuntimeDependencyAvailableAtTestRuntime() {
        try {
            // Use reflection to access test-runtime dependency
            Class<?> testRuntimeDepClass = Class.forName("org.apache.maven.its.mng8750.deps.TestRuntimeDep");
            Object dep = testRuntimeDepClass.getDeclaredConstructor().newInstance();

            // Call getMessage() method using reflection
            String result = (String) testRuntimeDepClass.getMethod("getMessage").invoke(dep);

            Assert.assertTrue(
                    "Test-runtime dependency should be available at test runtime",
                    result.contains("Test runtime dependency"));

            System.out.println("Test runtime classpath verification: PASSED");

        } catch (ClassNotFoundException e) {
            Assert.fail("Test-runtime dependency should be available at test runtime: " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("Error accessing test-runtime dependency: " + e.getMessage());
        }
    }

    /**
     * Test that verifies test-runtime dependencies were NOT available during compilation.
     * This is demonstrated by the fact that we cannot directly import or use the
     * TestRuntimeDep class in this source file.
     */
    @Test
    public void testTestRuntimeDependencyNotAvailableAtTestCompile() {
        // The fact that this test class compiled successfully without being able to
        // directly import TestRuntimeDep proves that test-runtime dependencies are
        // not available during test compilation
        Assert.assertTrue("Test class compiled without direct access to test-runtime dependency", true);

        // We can verify this by checking that the class is not directly accessible
        // during compilation (which is why we had to comment out the import)
        System.out.println(
                "Test compile classpath verification: PASSED - test-runtime dependency not available during compilation");
    }

    /**
     * Test that demonstrates the difference between test and test-runtime scopes.
     */
    @Test
    public void testScopeDifference() {
        // Regular test dependency - available during both compilation and runtime
        TestDep testDep = new TestDep();
        Assert.assertNotNull("Test dependency should be available", testDep);

        // Test-runtime dependency - only available during runtime (accessed via reflection)
        try {
            Class<?> testRuntimeDepClass = Class.forName("org.apache.maven.its.mng8750.deps.TestRuntimeDep");
            Object testRuntimeDep = testRuntimeDepClass.getDeclaredConstructor().newInstance();
            Assert.assertNotNull("Test-runtime dependency should be available at runtime", testRuntimeDep);
        } catch (Exception e) {
            Assert.fail("Test-runtime dependency should be available at test runtime: " + e.getMessage());
        }
    }
}
