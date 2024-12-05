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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1898">MNG-1898</a>.
 *
 * @author John Casey
 *
 */
public class MavenIT0086PluginRealmTest extends AbstractMavenIntegrationTestCase {

    public MavenIT0086PluginRealmTest() {
        super("(2.0.2,)");
    }

    /**
     * Verify that a plugin dependency class/resource can be loaded from both the plugin classloader and the
     * context classloader available to the plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0086() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/it0086");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties pclProps = verifier.loadProperties("target/pcl.properties");
        assertNotNull(pclProps.getProperty("org.apache.maven.plugin.coreit.ClassA"));
        assertNotNull(pclProps.getProperty("org.apache.maven.plugin.coreit.ClassB"));
        assertNotNull(pclProps.getProperty("org.apache.maven.plugin.coreit.SomeClass"));
        assertNotNull(pclProps.getProperty("org/apache/maven/plugin/coreit/a.properties"));
        assertEquals("1", pclProps.getProperty("org/apache/maven/plugin/coreit/a.properties.count"));
        assertNotNull(pclProps.getProperty("org/apache/maven/plugin/coreit/b.properties"));
        assertEquals("1", pclProps.getProperty("org/apache/maven/plugin/coreit/b.properties.count"));
        assertNotNull(pclProps.getProperty("org/apache/maven/plugin/coreit/it.properties"));
        assertEquals("2", pclProps.getProperty("org/apache/maven/plugin/coreit/it.properties.count"));

        Properties tcclProps = verifier.loadProperties("target/tccl.properties");
        assertEquals(pclProps, tcclProps);
    }
}
