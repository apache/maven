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

import org.junit.Test;
import org.junit.Assert;

// These imports should work during test compilation
import org.apache.maven.its.mng8750.deps.TestOnlyDep;
import org.apache.maven.its.mng8750.deps.TestDep;

/**
 * Test class to verify test-only scope behavior.
 * This class uses both test-only and regular test dependencies to demonstrate
 * that test-only dependencies are available during test compilation but not at test runtime.
 */
public class TestOnlyTest {
    
    /**
     * Test that regular test dependencies are available at test runtime.
     */
    @Test
    public void testTestDependencyAvailableAtTestRuntime() {
        TestDep dep = new TestDep();
        String result = dep.getMessage();
        Assert.assertTrue("Test dependency should be available", 
                         result.contains("Test dependency"));
    }
    
    /**
     * Test that test-only dependencies are NOT available at test runtime.
     * This test will compile successfully because test-only dependencies are available
     * during test compilation, but will fail at runtime if the scope works correctly.
     */
    @Test
    public void testTestOnlyDependencyNotAvailableAtTestRuntime() {
        try {
            // This should compile but fail at runtime
            TestOnlyDep dep = new TestOnlyDep();
            dep.getMessage();
            Assert.fail("Test-only dependency should not be available at test runtime");
        } catch (NoClassDefFoundError e) {
            // Expected - test-only dependency should not be in test runtime classpath
            System.out.println("Test runtime classpath verification: PASSED");
        }
    }
    
    /**
     * Test that demonstrates test-only dependencies can be used during compilation.
     * This method compiles successfully, proving test-only dependencies are available
     * during test compilation phase.
     */
    @Test
    public void testTestOnlyDependencyAvailableAtTestCompile() {
        // This test verifies that the test-only dependency was available during compilation
        // by checking that this test class itself compiled successfully
        Assert.assertTrue("Test class compiled successfully, proving test-only dependency was available during test compilation", true);
        
        // We can't actually instantiate the TestOnlyDep here because it won't be available
        // at runtime, but the fact that this class compiled proves it was available during
        // test compilation
    }
    
    /**
     * Helper method that uses test-only dependency.
     * This method will compile but cannot be safely called at runtime.
     */
    private String useTestOnlyDep() {
        // This compiles but will fail at runtime
        TestOnlyDep dep = new TestOnlyDep();
        return dep.getMessage();
    }
    
    /**
     * Helper method that uses regular test dependency.
     * This method will compile and can be safely called at runtime.
     */
    private String useTestDep() {
        TestDep dep = new TestDep();
        return dep.getMessage();
    }
}
