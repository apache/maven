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
package org.apache.maven.plugin.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPluginValidationManagerTest {

    /**
     * The location of the super POM is the URL of a resource in a JAR. It is not a file path, and on Windows
     * converting it to one fails with an {@code InvalidPathException} (MNG-8504).
     */
    @Test
    void testUrlLocationsAreNotFilePaths() {
        assertTrue(DefaultPluginValidationManager.isUrl("jar:file:/C:/maven/lib/maven-model-builder.jar!/pom.xml"));
        assertTrue(
                DefaultPluginValidationManager.isUrl("jar:file:/usr/share/maven/lib/maven-model-builder.jar!/pom.xml"));
        assertTrue(DefaultPluginValidationManager.isUrl("file:/home/user/project/pom.xml"));
        assertTrue(DefaultPluginValidationManager.isUrl("https://repo.example.org/pom.xml"));
    }

    @Test
    void testFilePathsAreNotUrls() {
        assertFalse(DefaultPluginValidationManager.isUrl("/home/user/project/pom.xml"));
        assertFalse(DefaultPluginValidationManager.isUrl("C:\\Users\\user\\project\\pom.xml"));
        assertFalse(DefaultPluginValidationManager.isUrl("C:/Users/user/project/pom.xml"));
        assertFalse(DefaultPluginValidationManager.isUrl("pom.xml"));
        assertFalse(DefaultPluginValidationManager.isUrl("child/pom.xml"));
    }
}
