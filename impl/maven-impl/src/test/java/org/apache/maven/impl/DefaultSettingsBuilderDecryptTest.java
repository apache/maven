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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a settings decryption failure is reported without echoing the encrypted value that
 * failed to decrypt, and that the credential is instead identified by its server/proxy/profile
 * reference.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSettingsBuilderDecryptTest {

    // corrupted on purpose: not a valid encrypted value for the master password below
    private static final String CORRUPTED_ENCRYPTED_VALUE =
            "{L6L/HbmrY+cH+sNkphn-this password is corrupted intentionally-q3fguYepTpM04WlIXb8nB1pk=}";

    @Mock
    Session session;

    @Mock
    Dispatcher dispatcher;

    @BeforeEach
    void setup() {
        Mockito.lenient()
                .when(session.getService(SettingsXmlFactory.class))
                .thenReturn(new DefaultSettingsXmlFactory());
        Mockito.lenient()
                .when(session.getEffectiveProperties())
                .thenReturn(Map.of(
                        Constants.MAVEN_SETTINGS_SECURITY,
                        securitySettingsPath().toString()));
    }

    @Test
    void decryptFailureMessageExcludesEncryptedValueAndIdentifiesTheServer() {
        SettingsBuilder builder = new DefaultSettingsBuilder(
                new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of("test", dispatcher));

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Sources.buildSource(getSettings("settings-servers-decrypt-fail")))
                .build();

        SettingsBuilderException exception = assertThrows(SettingsBuilderException.class, () -> builder.build(request));

        String message = exception.getMessage();
        assertFalse(
                message.contains(CORRUPTED_ENCRYPTED_VALUE),
                "decrypt-failure message must not contain the encrypted value: " + message);
        assertTrue(
                message.contains("for server corrupted-server"),
                "decrypt-failure message should identify the credential by server id: " + message);
    }

    private Path getSettings(String name) {
        return Paths.get("src/test/resources/settings/" + name + ".xml").toAbsolutePath();
    }

    private Path securitySettingsPath() {
        return Paths.get("src/test/resources/settings/settings-security-decrypt.xml")
                .toAbsolutePath();
    }
}
