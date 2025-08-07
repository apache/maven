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
 * Example class that uses both compile-only and regular compile dependencies.
 * This demonstrates that compile-only dependencies are available during compilation.
 */
public class CompileOnlyExample {

    /**
     * Method that uses a compile-only dependency.
     * This should compile successfully but the dependency won't be available at runtime.
     */
    public String useCompileOnlyDep() {
        CompileOnlyDep dep = new CompileOnlyDep();
        return "Used compile-only dependency: " + dep.getMessage();
    }

    /**
     * Method that uses a regular compile dependency.
     * This should compile successfully and the dependency will be available at runtime.
     */
    public String useCompileDep() {
        CompileDep dep = new CompileDep();
        return "Used compile dependency: " + dep.getMessage();
    }

    /**
     * Main method for testing.
     */
    public static void main(String[] args) {
        CompileOnlyExample example = new CompileOnlyExample();

        // This will work during compilation
        System.out.println(example.useCompileDep());

        // This will also work during compilation but fail at runtime
        // if compile-only dependency is not in runtime classpath
        try {
            System.out.println(example.useCompileOnlyDep());
            System.out.println("ERROR: Compile-only dependency should not be available at runtime!");
        } catch (NoClassDefFoundError e) {
            System.out.println(
                    "Runtime classpath verification: PASSED - compile-only dependency not available at runtime");
        }
    }
}
