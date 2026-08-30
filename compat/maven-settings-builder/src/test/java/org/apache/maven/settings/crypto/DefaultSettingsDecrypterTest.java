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
package org.apache.maven.settings.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@Deprecated
class DefaultSettingsDecrypterTest {

    @Test
    void testDecryptionDoesNotMutateCallerObjects() {
        String ciphertext = "{COQLCE6DU6GtcS5P=}";

        Server server = new Server();
        server.setId("test-server");
        server.setPassword(ciphertext);

        Proxy proxy = new Proxy();
        proxy.setId("test-proxy");
        proxy.setPassword(ciphertext);

        DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest();
        request.setServers(new ArrayList<>(List.of(server)));
        request.setProxies(new ArrayList<>(List.of(proxy)));

        DefaultSettingsDecrypter decrypter = new DefaultSettingsDecrypter(new MavenSecDispatcher(Map.of()));
        SettingsDecryptionResult result = decrypter.decrypt(request);

        // decryption state (including plaintext on success) must only ever land in the
        // result copies, never in the caller's live settings objects
        assertNotSame(server, result.getServers().get(0));
        assertNotSame(proxy, result.getProxies().get(0));
        assertEquals(ciphertext, server.getPassword());
        assertEquals(ciphertext, proxy.getPassword());
        assertEquals("test-proxy", result.getProxies().get(0).getId());
    }
}
