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
package org.apache.maven.model.building;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class DefaultModelBuilderFactoryTest {

    private static final String BASE_DIR =
            Paths.get("src", "test", "resources", "poms", "factory").toString();

    private File getPom(String name) {
        return new File(Paths.get(BASE_DIR, name + ".xml").toString()).getAbsoluteFile();
    }

    @Test
    void testCompleteWiring() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setPomFile(getPom("simple"));

        ModelBuildingResult result = builder.build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveModel());
        assertEquals("activated", result.getEffectiveModel().getProperties().get("profile.file"));
        Xpp3Dom conf = (Xpp3Dom)
                result.getEffectiveModel().getBuild().getPlugins().get(0).getConfiguration();
        assertNotNull(conf);
        assertEquals("1.5", conf.getChild("source").getValue());
        assertEquals("  1.5  ", conf.getChild("target").getValue());
    }

    @Test
    void testPomChanges() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);
        File pom = getPom("simple");

        String originalExists =
                readPom(pom).getProfiles().get(1).getActivation().getFile().getExists();

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setPomFile(pom);
        ModelBuildingResult result = builder.build(request);
        String resultExists = result.getRawModel()
                .getProfiles()
                .get(1)
                .getActivation()
                .getFile()
                .getExists();

        assertEquals(originalExists, resultExists);
        assertTrue(result.getEffectiveModel()
                .getProfiles()
                .get(1)
                .getActivation()
                .getFile()
                .getExists()
                .contains(BASE_DIR));
    }

    private static Model readPom(File file) throws Exception {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            return new MavenStaxReader().read(is);
        }
    }
}
