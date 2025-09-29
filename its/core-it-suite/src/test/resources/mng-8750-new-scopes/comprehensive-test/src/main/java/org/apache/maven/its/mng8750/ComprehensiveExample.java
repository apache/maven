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

import org.apache.maven.its.mng8750.deps.CompileDep;
import org.apache.maven.its.mng8750.deps.CompileOnlyDep;

/**
 * Comprehensive example class that demonstrates all new Maven 4 scopes.
 * This class uses compile-only and regular compile dependencies.
 */
public class ComprehensiveExample {

    /**
     * Method that uses a compile-only dependency.
     */
    public String useCompileOnlyDep() {
        CompileOnlyDep dep = new CompileOnlyDep();
        return "Used compile-only dependency: " + dep.getMessage();
    }

    /**
     * Method that uses a regular compile dependency.
     */
    public String useCompileDep() {
        CompileDep dep = new CompileDep();
        return "Used compile dependency: " + dep.getMessage();
    }

    /**
     * Main method for comprehensive testing.
     */
    public static void main(String[] args) {
        ComprehensiveExample example = new ComprehensiveExample();

        System.out.println("=== Comprehensive Scope Test ===");

        // Test regular compile dependency (should work at runtime)
        try {
            System.out.println(example.useCompileDep());
            System.out.println("Compile scope verification: PASSED");
        } catch (Exception e) {
            System.out.println("Compile scope verification: FAILED - " + e.getMessage());
        }

        // Test compile-only dependency (should fail at runtime)
        try {
            System.out.println(example.useCompileOnlyDep());
            System.out.println("Compile-only scope verification: FAILED - should not be available at runtime");
        } catch (NoClassDefFoundError e) {
            System.out.println("Compile-only scope verification: PASSED - not available at runtime");
        }

        System.out.println("=== End Comprehensive Scope Test ===");
    }
}
