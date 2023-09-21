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
package org.apache.maven.model.merge;

import java.util.Collections;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Prerequisites;
import org.apache.maven.api.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MavenModelMergerTest {
    private MavenModelMerger modelMerger = new MavenModelMerger();

    // modelVersion is neither inherited nor injected
    @Test
    void testMergeModel_ModelVersion() {
        Model parent = Model.newBuilder().modelVersion("4.0.0").build();
        Model model = Model.newInstance();
        Model.Builder builder = Model.newBuilder(model);
        modelMerger.mergeModel_ModelVersion(builder, model, parent, false, null);
        assertNull(builder.build().getModelVersion());

        model = Model.newBuilder().modelVersion("5.0.0").build();
        builder = Model.newBuilder(model);
        modelMerger.mergeModel_ModelVersion(builder, model, parent, false, null);
        assertEquals("5.0.0", builder.build().getModelVersion());
    }

    // ArtifactId is neither inherited nor injected
    @Test
    void testMergeModel_ArtifactId() {
        Model parent = Model.newBuilder().artifactId("PARENT").build();
        Model model = Model.newInstance();
        Model.Builder builder = Model.newBuilder(model);
        modelMerger.mergeModel_ArtifactId(builder, model, parent, false, null);
        assertNull(model.getArtifactId());

        model = Model.newBuilder().artifactId("MODEL").build();
        builder = Model.newBuilder(model);
        modelMerger.mergeModel_ArtifactId(builder, model, parent, false, null);
        assertEquals("MODEL", builder.build().getArtifactId());
    }

    // Prerequisites are neither inherited nor injected
    @Test
    void testMergeModel_Prerequisites() {
        Model parent =
                Model.newBuilder().prerequisites(Prerequisites.newInstance()).build();
        Model model = Model.newInstance();
        Model.Builder builder = Model.newBuilder(model);
        modelMerger.mergeModel_Prerequisites(builder, model, parent, false, null);
        assertNull(builder.build().getPrerequisites());

        Prerequisites modelPrerequisites =
                Prerequisites.newBuilder().maven("3.0").build();
        model = Model.newBuilder().prerequisites(modelPrerequisites).build();
        builder = Model.newBuilder(model);
        modelMerger.mergeModel_Prerequisites(builder, model, parent, false, null);
        assertEquals(modelPrerequisites, builder.build().getPrerequisites());
    }

    // Profiles are neither inherited nor injected
    @Test
    void testMergeModel_Profiles() {
        Model parent = Model.newBuilder()
                .profiles(Collections.singletonList(Profile.newInstance()))
                .build();
        Model model = Model.newInstance();
        Model.Builder builder = Model.newBuilder(model);
        modelMerger.mergeModel_Profiles(builder, model, parent, false, null);
        assertEquals(0, builder.build().getProfiles().size());

        Profile modelProfile = Profile.newBuilder().id("MODEL").build();
        model = Model.newBuilder()
                .profiles(Collections.singletonList(modelProfile))
                .build();
        builder = Model.newBuilder(model);
        modelMerger.mergeModel_Prerequisites(builder, model, parent, false, null);
        assertEquals(Collections.singletonList(modelProfile), builder.build().getProfiles());
    }
}
