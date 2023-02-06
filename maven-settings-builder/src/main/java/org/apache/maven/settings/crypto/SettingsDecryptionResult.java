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

import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;

/**
 * Collects the output of the settings decrypter.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsDecryptionResult {

    /**
     * Gets the decrypted server. This is a convenience method to retrieve the first element from {@link #getServers()}.
     *
     * @return The decrypted server or {@code null}.
     */
    Server getServer();

    /**
     * Gets the decrypted servers.
     *
     * @return The decrypted server, can be empty but never {@code null}.
     */
    List<Server> getServers();

    /**
     * Gets the decrypted proxy. This is a convenience method to retrieve the first element from {@link #getProxies()}.
     *
     * @return The decrypted proxy or {@code null}.
     */
    Proxy getProxy();

    /**
     * Gets the decrypted proxies.
     *
     * @return The decrypted proxy, can be empty but never {@code null}.
     */
    List<Proxy> getProxies();

    /**
     * Gets the problems that were encountered during the settings decryption.
     *
     * @return The problems that were encountered during the decryption, can be empty but never {@code null}.
     */
    List<SettingsProblem> getProblems();
}
