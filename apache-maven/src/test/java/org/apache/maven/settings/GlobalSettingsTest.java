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
package org.apache.maven.settings;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.maven.api.settings.InputSource;
import org.apache.maven.settings.v4.SettingsStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the global settings.xml shipped with the distribution is in good state.
 *
 */
class GlobalSettingsTest {

    @Test
    void testValidGlobalSettings() throws Exception {
        String basedir = System.getProperty("basedir", System.getProperty("user.dir"));

        File globalSettingsFile = new File(basedir, "src/assembly/maven/conf/settings.xml");
        assertTrue(globalSettingsFile.isFile(), globalSettingsFile.getAbsolutePath());

        try (InputStream is = Files.newInputStream(globalSettingsFile.toPath())) {
            new SettingsStaxReader().read(is, true, new InputSource(globalSettingsFile.getAbsolutePath()));
        }
    }
}
