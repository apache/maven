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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3813">MNG-3813</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3813PluginClassPathOrderingTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3813PluginClassPathOrderingTest() {
        super("(2.0.8,)");
    }

    /**
     * Verify that the ordering of the plugin class path matches the ordering of the dependencies as given in the POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3813() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3813");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3813");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties pclProps = verifier.loadProperties("target/pcl.properties");

        String className = "org.apache.maven.its.mng3813.SomeClass";
        String resName = className.replace('.', '/') + ".class";

        assertEquals("depA", pclProps.getProperty(className + ".methods"));
        assertTrue(pclProps.getProperty(resName).endsWith("/dep-a-0.1.jar!/" + resName));

        assertEquals("8", pclProps.getProperty(resName + ".count"));

        assertTrue(pclProps.getProperty(resName + ".0").endsWith("/dep-a-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".1").endsWith("/dep-aa-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".2").endsWith("/dep-ac-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".3").endsWith("/dep-ab-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".4").endsWith("/dep-ad-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".5").endsWith("/dep-c-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".6").endsWith("/dep-b-0.1.jar!/" + resName));
        assertTrue(pclProps.getProperty(resName + ".7").endsWith("/dep-d-0.1.jar!/" + resName));
    }
}
