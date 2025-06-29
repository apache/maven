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
package org.apache.maven.project.interpolation;

import java.util.Map;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.junit.jupiter.api.Test;

class StringSearchModelInterpolatorTest {

    @Test
    void interpolate() throws ModelInterpolationException, InitializationException {
        Model model = Model.newBuilder()
                .groupId("group")
                .location("groupId", InputLocation.of(InputSource.of("model", null)))
                .build();
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator();
        interpolator.initialize();
        interpolator.interpolate(new org.apache.maven.model.Model(model), Map.of());
    }
}
