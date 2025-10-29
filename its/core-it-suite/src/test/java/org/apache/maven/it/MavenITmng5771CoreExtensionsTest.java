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
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5771">MNG-5771</a>:
 * check that Maven loads core extensions and components contributed by <code>.mvn/extensions.xml</code>
 * are available to regular plugins.
 */
public class MavenITmng5771CoreExtensionsTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testCoreExtension() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-5771-core-extensions");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.filterFile("settings-template.xml", "settings.xml");

        verifier = newVerifier(testDir.resolve("client").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-core-extensions");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(testDir.resolve("settings.xml").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testCoreExtensionNoDescriptor() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-5771-core-extensions");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.filterFile("settings-template.xml", "settings.xml");

        verifier = newVerifier(testDir.resolve("client-no-descriptor").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-core-extensions");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(testDir.resolve("settings.xml").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-5795: Maven extensions can not be retrieved from authenticated
    // repositories
    //
    @Test
    public void testCoreExtensionRetrievedFromAMirrorWithBasicAuthentication() throws Exception {
        // requiresMavenVersion("[3.3.2,)");

        Path testDir = extractResourcesAsPath("/mng-5771-core-extensions");

        HttpServer server = HttpServer.builder() //
                .port(0) //
                .username("maven") //
                .password("secret") //
                .source(testDir.resolve("repo")) //
                .build();
        server.start();

        Verifier verifier = newVerifier(testDir.toString());
        Map<String, String> properties = verifier.newDefaultFilterMap();
        properties.put("@port@", Integer.toString(server.port()));
        String mirrorOf = "*";
        properties.put("@mirrorOf@", mirrorOf);
        verifier.filterFile("settings-template-mirror-auth.xml", "settings.xml", properties);

        verifier = newVerifier(testDir.resolve("client").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-core-extensions");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(testDir.resolve("settings.xml").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        server.stop();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-7395: Support properties in extensions.xml
    //
    @Test
    public void testCoreExtensionWithProperties() throws Exception {
        // requiresMavenVersion("[3.8.5,)");

        Path testDir = extractResourcesAsPath("/mng-5771-core-extensions");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.filterFile("settings-template.xml", "settings.xml");

        verifier = newVerifier(testDir.resolve("client-properties").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-core-extensions");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(testDir.resolve("settings.xml").getAbsolutePath());
        verifier.addCliArgument("-Dtest-extension-version=0.1");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-7395: Support properties in extensions.xml
    //
    @Test
    public void testCoreExtensionWithConfig() throws Exception {
        // requiresMavenVersion("[3.8.5,)");

        Path testDir = extractResourcesAsPath("/mng-5771-core-extensions");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.filterFile("settings-template.xml", "settings.xml");

        verifier = newVerifier(testDir.resolve("client-config").getAbsolutePath());
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it-core-extensions");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(testDir.resolve("settings.xml").getAbsolutePath());
        verifier.setForkJvm(true); // force forked JVM since we need the shell script to detect .mvn/
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
