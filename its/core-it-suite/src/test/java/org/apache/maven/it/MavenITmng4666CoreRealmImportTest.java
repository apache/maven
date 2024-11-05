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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4666">MNG-4666</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4666CoreRealmImportTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4666CoreRealmImportTest() {
        super("[2.0.11,2.0.99),[2.1.0,3.0-alpha-1),[3.0-beta-2,)");
    }

    /**
     * Verify that API types from the Maven core realm are shared/imported into the plugin realm despite the plugin
     * declaring conflicting dependencies. For the core artifact filter, this boils down to the filter properly
     * recognizing such a conflicting dependency, i.e. knowing the relevant groupId:artifactId's.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4666");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven", "maven-model", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-settings", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-project", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-artifact", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-core", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-plugin-api", "0.1-stub");
        verifier.deleteArtifacts("org.apache.maven", "maven-plugin-descriptor", "0.1-stub");
        verifier.deleteArtifacts("plexus", "plexus-container-default", "0.1-stub");
        verifier.deleteArtifacts("org.codehaus.plexus", "plexus-container-default", "0.1-stub");
        verifier.deleteArtifacts("org.codehaus.plexus", "plexus-component-api", "0.1-stub");
        verifier.deleteArtifacts("org.codehaus.plexus", "plexus-utils", "0.1-stub");
        verifier.deleteArtifacts("org.codehaus.plexus", "plexus-classworlds", "0.1-stub");
        verifier.deleteArtifacts("org.sonatype.aether", "aether-api", "0.1-stub");
        verifier.deleteArtifacts("org.sonatype.aether", "aether-spi", "0.1-stub");
        verifier.deleteArtifacts("org.sonatype.aether", "aether-impl", "0.1-stub");
        verifier.deleteArtifacts("org.sonatype.sisu", "sisu-inject-plexus", "0.1-stub");
        verifier.deleteArtifacts("org.sonatype.spice", "spice-inject-plexus", "0.1-stub");
        verifier.deleteArtifacts("classworlds", "classworlds", "0.1-stub");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/type.properties");
        List<String> types = getTypes(props);
        if (!matchesVersionRange("[3.0-beta-4,)")) {
            // MNG-4725, MNG-4807
            types.remove("org.codehaus.plexus.configuration.PlexusConfiguration");
            types.remove("org.codehaus.plexus.logging.Logger");
        }
        assertFalse(types.isEmpty());
        for (String type : types) {
            assertEquals(type, props.get("plugin." + type), props.get("core." + type));
        }
    }

    private List<String> getTypes(Properties props) {
        List<String> types = new ArrayList<>();
        for (Object o : props.keySet()) {
            String key = o.toString();
            if (key.startsWith("core.")) {
                String type = key.substring(5);
                if (props.getProperty(key, "").length() > 0) {
                    // types not in the core realm can't be exported/shared, so ignore those
                    types.add(type);
                }
            }
        }
        return types;
    }
}
