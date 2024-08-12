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
package org.apache.maven.model.profile;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests model builder profile interpolation.
 */
public class DefaultProfileInterpolationTest {
    private File getPom(String name) {
        return new File("src/test/resources/poms/profile/" + name);
    }

    /**
     * MNG-8188: profile interpolation was "undone" by mistake. This UT executes reproducer and ensures that
     * profile interpolated values (sans activation) are fully interpolated.
     */
    @Test
    public void profilePropertiesInterpolation() throws Exception {
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new FileModelSource(getPom("mng8188.xml")));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        ModelBuildingResult result = builder.build(request);
        assertNotNull(result);
        Model effectiveModel = result.getEffectiveModel();
        assertNotNull(effectiveModel);

        Plugin interpolatedPlugin = null;

        // build/pluginManagement
        for (Plugin plugin : effectiveModel.getBuild().getPluginManagement().getPlugins()) {
            if ("spring-boot-maven-plugin".equals(plugin.getArtifactId())) {
                interpolatedPlugin = plugin;
                break;
            }
        }
        assertNotNull(interpolatedPlugin);
        assertEquals("3.3.1", interpolatedPlugin.getVersion());

        // profiles/foo/build/pluginManagement
        interpolatedPlugin = null;
        for (Plugin plugin : effectiveModel
                .getProfiles()
                .get(0)
                .getBuild()
                .getPluginManagement()
                .getPlugins()) {
            if ("spring-boot-maven-plugin".equals(plugin.getArtifactId())) {
                interpolatedPlugin = plugin;
                break;
            }
        }
        assertNotNull(interpolatedPlugin);
        assertEquals("3.3.1", interpolatedPlugin.getVersion());
    }
}
