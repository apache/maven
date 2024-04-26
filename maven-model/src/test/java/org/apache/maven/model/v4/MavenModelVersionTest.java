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
package org.apache.maven.model.v4;

import java.io.InputStream;
import java.util.Collections;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenModelVersionTest {

    private static Model model;

    @BeforeAll
    static void setup() throws Exception {
        try (InputStream is = MavenModelVersionTest.class.getResourceAsStream("/xml/pom.xml")) {
            model = new MavenStaxReader().read(is);
        }
    }

    @Test
    void testV4Model() {
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(model));
    }

    @Test
    void testV4ModelVersion() {
        Model m = model.withModelVersion("4.1.0");
        assertEquals("4.0.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelRoot() {
        Model m = model.withRoot(true);
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelPreserveModelVersion() {
        Model m = model.withPreserveModelVersion(true);
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m));
    }

    @Test
    void testV4ModelPriority() {
        Model m = model.withBuild(Build.newInstance()
                .withPlugins(Collections.singleton(Plugin.newInstance()
                        .withExecutions(Collections.singleton(
                                PluginExecution.newInstance().withPriority(5))))));
        assertEquals("4.1.0", new MavenModelVersion().getModelVersion(m));
    }
}
