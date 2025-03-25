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
import java.nio.file.Paths;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8645">MNG-8645</a>.
 */
class MavenITmng8645ConsumerPomDependencyManagementTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8645ConsumerPomDependencyManagementTest() {
        super("(4.0.0-rc-3,)");
    }

    /**
     *  Verify the dependency management of the consumer POM is computed correctly
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8645-consumer-pom-dep-mgmt")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // validate consumer pom
        Path consumerPom = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.mng8645",
                "test",
                "1.0",
                "test-1.0-consumer.pom"));
        assertTrue(Files.exists(consumerPom));
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }
        assertNotNull(consumerPomModel.getDependencyManagement());
        assertEquals(
                1, consumerPomModel.getDependencyManagement().getDependencies().size());
    }
}
