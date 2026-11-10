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
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/13761">GH-13761</a>.
 * The consumer POM carries the property values as declared, in the main block and in each
 * profile, whatever profiles were active on the machine that built the project. The properties
 * of those profiles only serve interpolation and coordinate validation (MNG-8709).
 */
class MavenITgh13761ConsumerPomProfilePropertiesTest extends AbstractMavenIntegrationTestCase {

    private static final String GROUP_ID = "org.apache.maven.its.gh13761";

    @Test
    void testActiveProfilePropertiesAreNotPersistedInConsumerPom() throws Exception {
        Path basedir = extractResources("gh-13761-consumer-pom-profile-properties");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Model consumerPom = read(verifier.getArtifactPath(GROUP_ID, "profile-properties", "1.0", "pom"));

        assertEquals(Map.of("flag", "false", "classifier", "none"), consumerPom.getProperties());
        assertEquals(Map.of("flag", "true", "classifier", "here"), profile(consumerPom, "active-here").getProperties());
        assertEquals(Map.of("flag", "false"), profile(consumerPom, "opt-in").getProperties());
        assertEquals(Map.of("classifier", "elsewhere"), profile(consumerPom, "elsewhere").getProperties());
    }

    /**
     * A property passed on the command line still overrides the declared value, as before.
     */
    @Test
    void testUserPropertyStillOverridesDeclaredValueInConsumerPom() throws Exception {
        Path basedir = extractResources("gh-13761-consumer-pom-profile-properties");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArgument("-Dclassifier=cli");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Model consumerPom = read(verifier.getArtifactPath(GROUP_ID, "profile-properties", "1.0", "pom"));

        assertEquals(Map.of("flag", "false", "classifier", "cli"), consumerPom.getProperties());
        assertEquals(Map.of("flag", "false"), profile(consumerPom, "opt-in").getProperties());
    }

    private static Model read(Path pomFile) throws Exception {
        try (Reader reader = Files.newBufferedReader(pomFile)) {
            return new MavenStaxReader().read(reader);
        }
    }

    private static Profile profile(Model model, String id) {
        return model.getProfiles().stream()
                .filter(profile -> id.equals(profile.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("profile " + id + " is missing from the consumer POM"));
    }
}
