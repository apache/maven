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
package org.apache.maven.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify NIO2 migration of AbstractMavenIntegrationTestCase class.
 */
public class AbstractMavenIntegrationTestCaseNIO2Test extends AbstractMavenIntegrationTestCase {

    @Test
    void testExtractResourcesAsPath() throws IOException {
        String resourcePath = "test-resource";
        
        // Test the new Path-based method
        Path result = extractResources(resourcePath);
        
        assertNotNull(result);
        assertTrue(result.isAbsolute());
        assertTrue(result.toString().endsWith(resourcePath));
    }

    @Test
    void testPathToFileConversion() throws IOException {
        String resourcePath = "test-resource";

        // Test that Path can be converted to File when needed
        Path pathResult = extractResources(resourcePath);
        File fileResult = pathResult.toFile();

        assertNotNull(fileResult);
        assertTrue(fileResult.isAbsolute());
        assertTrue(fileResult.getPath().endsWith(resourcePath));

        // Verify round-trip conversion
        assertEquals(pathResult, fileResult.toPath());
    }
}
