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
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.internal.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 */
@ExtendWith(MockitoExtension.class)
class DefaultSettingsBuilderFactoryTest {

    @Mock
    Session session;

    @BeforeEach
    void setup() {
        Map<String, String> map = System.getProperties().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(), e -> e.getValue().toString()));
        //        lenient().when(session.getSystemProperties()).thenReturn(map);
        //        lenient().when(session.getUserProperties()).thenReturn(Collections.emptyMap());

        Mockito.lenient()
                .when(session.getService(SettingsXmlFactory.class))
                .thenReturn(new DefaultSettingsXmlFactory());
    }

    @Test
    void testCompleteWiring() {
        SettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        assertNotNull(builder);

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Source.fromPath(getSettings("settings-simple")))
                .build();

        SettingsBuilderResult result = builder.build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveSettings());
    }

    private Path getSettings(String name) {
        return Paths.get("src/test/resources/" + name + ".xml").toAbsolutePath();
    }
}
