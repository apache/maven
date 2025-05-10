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

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8414">MNG-8414</a>.
 */
class MavenITmng8414ConsumerPomWithNewFeaturesTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8414ConsumerPomWithNewFeaturesTest() {
        super("[4.0.0-rc-2,)");
    }

    /**
     *  Verify behavior of the consumer POM when using a feature that require a newer model.
     */
    @Test
    void testNotPreserving() throws Exception {
        Path basedir =
                extractResources("/mng-8414-consumer-pom-with-new-features").toPath();

        Verifier verifier = newVerifier(basedir.toString(), null);
        verifier.addCliArguments("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog(
                "The consumer POM for org.apache.maven.its:mng-8414:jar:1.0.0-SNAPSHOT cannot be downgraded to 4.0.0.");

        Path consumerPom = basedir.resolve(Path.of(
                "target",
                "project-local-repo",
                "org.apache.maven.its",
                "mng-8414",
                "1.0.0-SNAPSHOT",
                "mng-8414-1.0.0-SNAPSHOT-consumer.pom"));
        assertTrue(Files.exists(consumerPom));
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }
        assertEquals("4.1.0", consumerPomModel.getModelVersion());
    }

    /**
     *  Verify behavior of the consumer POM when using a feature that require a newer model.
     */
    @Test
    void testPreserving() throws Exception {
        Path basedir =
                extractResources("/mng-8414-consumer-pom-with-new-features").toPath();

        Verifier verifier = newVerifier(basedir.toString(), null);
        verifier.setLogFileName("log-preserving.txt");
        verifier.addCliArguments("-f", "pom-preserving.xml", "package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextNotInLog("cannot be downgraded to 4.0.0.");

        Path consumerPom = basedir.resolve(Path.of(
                "target",
                "project-local-repo",
                "org.apache.maven.its",
                "mng-8414-preserving",
                "1.0.0-SNAPSHOT",
                "mng-8414-preserving-1.0.0-SNAPSHOT-consumer.pom"));
        assertTrue(Files.exists(consumerPom));
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }
        assertEquals("4.1.0", consumerPomModel.getModelVersion());
    }
}
