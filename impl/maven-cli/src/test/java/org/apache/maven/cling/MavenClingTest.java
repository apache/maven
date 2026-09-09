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
package org.apache.maven.cling;

import java.io.IOException;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link MavenCling} main class dispatching.
 */
class MavenClingTest {

    /**
     * When {@code maven.mainClass} is set to a non-existent class, the delegation should throw
     * an {@link IOException} with a descriptive message.
     */
    @Test
    void delegatesToUnknownClassThrowsIOException() {
        String original = System.getProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
        try {
            System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, "com.example.NonExistentClass");
            ClassWorld world =
                    new ClassWorld(ClingSupport.CORE_CLASS_REALM_ID, getClass().getClassLoader());
            IOException ex = assertThrows(IOException.class, () -> MavenCling.main(new String[0], world));
            assertEquals("Cannot find maven.mainClass: com.example.NonExistentClass", ex.getMessage());
        } finally {
            if (original != null) {
                System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, original);
            } else {
                System.clearProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
            }
        }
    }

    /**
     * When {@code maven.mainClass} is set to a class that exists but does not have the
     * {@code main(String[], ClassWorld)} method, delegation should throw an {@link IOException}.
     */
    @Test
    void delegatesToClassWithoutMainMethodThrowsIOException() {
        String original = System.getProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
        try {
            // String.class exists but has no main(String[], ClassWorld)
            System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, "java.lang.String");
            ClassWorld world =
                    new ClassWorld(ClingSupport.CORE_CLASS_REALM_ID, getClass().getClassLoader());
            IOException ex = assertThrows(IOException.class, () -> MavenCling.main(new String[0], world));
            assertEquals(
                    "maven.mainClass does not have main(String[], ClassWorld) method: java.lang.String",
                    ex.getMessage());
        } finally {
            if (original != null) {
                System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, original);
            } else {
                System.clearProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
            }
        }
    }

    /**
     * When {@code maven.mainClass} is set to a valid class with the correct entry point,
     * delegation should invoke that class's main method and return its exit code.
     */
    @Test
    void delegatesToValidClass() throws IOException {
        String original = System.getProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
        try {
            System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, MavenClingTest.DelegateTarget.class.getName());
            ClassWorld world =
                    new ClassWorld(ClingSupport.CORE_CLASS_REALM_ID, getClass().getClassLoader());
            DelegateTarget.invoked = false;
            int exitCode = MavenCling.main(new String[] {"test-arg"}, world);
            assertEquals(42, exitCode);
            assertEquals(true, DelegateTarget.invoked);
        } finally {
            if (original != null) {
                System.setProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY, original);
            } else {
                System.clearProperty(MavenCling.MAVEN_MAIN_CLASS_PROPERTY);
            }
        }
    }

    /**
     * A test class that serves as a delegation target for {@link MavenCling}.
     */
    public static class DelegateTarget {
        static boolean invoked;

        @SuppressWarnings("unused")
        public static int main(String[] args, ClassWorld world) {
            invoked = true;
            return 42;
        }
    }
}
