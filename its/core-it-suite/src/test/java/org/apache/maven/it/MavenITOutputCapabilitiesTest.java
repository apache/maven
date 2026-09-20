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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenITOutputCapabilitiesTest extends AbstractMavenIntegrationTestCase {
    MavenITOutputCapabilitiesTest() {
        super("[4.0.0-SNAPSHOT,)");
    }

    @Test
    void injectsCapabilitiesIntoLegacyPluginsInParallelReactor() throws Exception {
        File directory = extractResources("/output-capabilities");
        Verifier plugin = newVerifier(new File(directory, "plugin").getPath());
        plugin.addCliArgument("install");
        plugin.execute();
        plugin.verifyErrorFreeLog();
        Verifier legacyPlugin = newVerifier(new File(directory, "legacy-plugin").getPath());
        legacyPlugin.addCliArgument("install");
        legacyPlugin.execute();
        legacyPlugin.verifyErrorFreeLog();

        for (String color : new String[] {"never", "always"}) {
            Verifier consumer = newVerifier(new File(directory, "consumer").getPath());
            consumer.addCliArgument("-T2");
            consumer.addCliArgument("-B");
            consumer.addCliArgument("--color=" + color);
            consumer.addCliArgument("validate");
            consumer.execute();
            consumer.verifyErrorFreeLog();
            for (String prefix : new String[] {"", "child/"}) {
                Properties properties = consumer.loadProperties(prefix + "target/output-capabilities.properties");
                Properties legacy = consumer.loadProperties(prefix + "target/legacy-output-capabilities.properties");
                assertEquals("true", legacy.getProperty("available"));
                assertEquals(properties.getProperty("destination"), legacy.getProperty("destination"));
                assertEquals(properties.getProperty("encoding"), legacy.getProperty("encoding"));
                // Maven 4's verifier directs logging through -l.
                assertEquals("FILE", properties.getProperty("destination"));
                assertEquals("UTF-8", properties.getProperty("encoding"));
            }
        }
    }
}
