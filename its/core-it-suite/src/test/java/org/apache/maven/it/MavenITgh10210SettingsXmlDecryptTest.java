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
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/10210">GH-10210</a>.
 * @since 4.0.0-rc4
 */
class MavenITgh10210SettingsXmlDecryptTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testItPass() throws Exception {
        File testDir = extractResources("/gh-10210-settings-xml-decrypt");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setUserHomeDirectory(testDir.toPath().resolve("HOME"));
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings-passes.xml");
        verifier.addCliArgument("process-resources");
        verifier.execute();

        Assert.assertEquals(
                Arrays.asList(
                        "prop1=%{foo}.txt",
                        "prop2=${foo}.txt",
                        "prop3=whatever {foo}.txt",
                        "prop4=whatever",
                        "prop5=Hello Oleg {L6L/HbmrY+cH+sNkphnq3fguYepTpM04WlIXb8nB1pk=} is this a password?",
                        "prop6=password",
                        "prop7=password"),
                Files.readAllLines(testDir.toPath().resolve("target/classes/file.properties")));
    }

    @Test
    void testItFail() throws Exception {
        File testDir = extractResources("/gh-10210-settings-xml-decrypt");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setUserHomeDirectory(testDir.toPath().resolve("HOME"));
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings-fails.xml");
        verifier.addCliArgument("process-resources");
        try {
            verifier.execute();
        } catch (VerificationException e) {
            Assert.assertTrue(
                    verifier.loadLogContent()
                            .contains(
                                    "Could not decrypt password (fix the corrupted password or remove it, if unused) {L6L/HbmrY+cH+sNkphn-this password is corrupted intentionally-q3fguYepTpM04WlIXb8nB1pk=}"));
        }
    }
}
