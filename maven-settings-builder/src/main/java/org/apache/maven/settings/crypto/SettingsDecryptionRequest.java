package org.apache.maven.settings.crypto;

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

import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;

/**
 * Collects parameters that control the decryption of settings.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsDecryptionRequest
{

    /**
     * Gets the servers whose passwords should be decrypted.
     *
     * @return The servers to decrypt, never {@code null}.
     */
    List<Server> getServers();

    /**
     * Sets the servers whose passwords should be decrypted.
     *
     * @param servers The servers to decrypt, may be {@code null}.
     * @return This request, never {@code null}.
     */
    SettingsDecryptionRequest setServers( List<Server> servers );

    /**
     * Gets the proxies whose passwords should be decrypted.
     *
     * @return The proxies to decrypt, never {@code null}.
     */
    List<Proxy> getProxies();

    /**
     * Sets the proxies whose passwords should be decrypted.
     *
     * @param proxies The proxies to decrypt, may be {@code null}.
     * @return This request, never {@code null}.
     */
    SettingsDecryptionRequest setProxies( List<Proxy> proxies );

}
