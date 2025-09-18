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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MNG-11133: Mixins should override properties inherited from parent.
 */
public class MavenITmng11133MixinsPrecedenceTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng11133MixinsPrecedenceTest() {
        super("4.1.0"); // affected since 4.1.0+
    }

    @Test
    public void testMixinOverridesParentProperty() throws Exception {
        File testDir = extractResources("/mng-11133-mixins/project");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("help:effective-pom");
        verifier.addCliArgument("-Doutput=target/effective-pom.xml");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/effective-pom.xml");
        String effectivePom = verifier.loadFile("target/effective-pom.xml", false);
        assertTrue(effectivePom.contains("<maven.compiler.release>21</maven.compiler.release>"));
    }
}
