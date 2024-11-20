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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcherException;

/**
 * Decrypts passwords in the settings.
 *
 * @deprecated since 4.0.0
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultSettingsDecrypter implements SettingsDecrypter {
    private final SecDispatcher securityDispatcher;

    @Inject
    public DefaultSettingsDecrypter(MavenSecDispatcher securityDispatcher) {
        this.securityDispatcher = securityDispatcher;
    }

    @Override
    public SettingsDecryptionResult decrypt(SettingsDecryptionRequest request) {
        List<SettingsProblem> problems = new ArrayList<>();

        List<Server> servers = new ArrayList<>();

        for (Server server : request.getServers()) {
            server = server.clone();

            String password = server.getPassword();
            if (securityDispatcher.isAnyEncryptedString(password)) {
                try {
                    if (securityDispatcher.isLegacyEncryptedString(password)) {
                        problems.add(new DefaultSettingsProblem(
                                "Pre-Maven 4 legacy encrypted password detected for server " + server.getId()
                                        + " - configure password encryption with the help of mvnenc to be compatible with Maven 4.",
                                Severity.WARNING,
                                "server: " + server.getId(),
                                -1,
                                -1,
                                null));
                    }
                    server.setPassword(securityDispatcher.decrypt(password));
                } catch (SecDispatcherException | IOException e) {
                    problems.add(new DefaultSettingsProblem(
                            "Failed to decrypt password for server " + server.getId() + ": " + e.getMessage(),
                            Severity.ERROR,
                            "server: " + server.getId(),
                            -1,
                            -1,
                            e));
                }
            }

            String passphrase = server.getPassphrase();
            if (securityDispatcher.isAnyEncryptedString(passphrase)) {
                try {
                    if (securityDispatcher.isLegacyEncryptedString(passphrase)) {
                        problems.add(new DefaultSettingsProblem(
                                "Legacy/insecurely encrypted passphrase detected for server " + server.getId(),
                                Severity.WARNING,
                                "server: " + server.getId(),
                                -1,
                                -1,
                                null));
                    }
                    server.setPassphrase(securityDispatcher.decrypt(passphrase));
                } catch (SecDispatcherException | IOException e) {
                    problems.add(new DefaultSettingsProblem(
                            "Failed to decrypt passphrase for server " + server.getId() + ": " + e.getMessage(),
                            Severity.ERROR,
                            "server: " + server.getId(),
                            -1,
                            -1,
                            e));
                }
            }

            servers.add(server);
        }

        List<Proxy> proxies = new ArrayList<>();

        for (Proxy proxy : request.getProxies()) {
            String password = proxy.getPassword();
            if (securityDispatcher.isAnyEncryptedString(password)) {
                try {
                    if (securityDispatcher.isLegacyEncryptedString(password)) {
                        problems.add(new DefaultSettingsProblem(
                                "Legacy/insecurely encrypted password detected for proxy " + proxy.getId(),
                                Severity.WARNING,
                                "proxy: " + proxy.getId(),
                                -1,
                                -1,
                                null));
                    }
                    proxy.setPassword(securityDispatcher.decrypt(password));
                } catch (SecDispatcherException | IOException e) {
                    problems.add(new DefaultSettingsProblem(
                            "Failed to decrypt password for proxy " + proxy.getId() + ": " + e.getMessage(),
                            Severity.ERROR,
                            "proxy: " + proxy.getId(),
                            -1,
                            -1,
                            e));
                }
            }

            proxies.add(proxy);
        }

        return new DefaultSettingsDecryptionResult(servers, proxies, problems);
    }
}
