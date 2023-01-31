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

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;

/**
 * Collects the output of the settings decrypter.
 *
 * @author Benjamin Bentmann
 */
class DefaultSettingsDecryptionResult implements SettingsDecryptionResult {

    private List<Server> servers;

    private List<Proxy> proxies;

    private List<SettingsProblem> problems;

    DefaultSettingsDecryptionResult(List<Server> servers, List<Proxy> proxies, List<SettingsProblem> problems) {
        this.servers = (servers != null) ? servers : new ArrayList<Server>();
        this.proxies = (proxies != null) ? proxies : new ArrayList<Proxy>();
        this.problems = (problems != null) ? problems : new ArrayList<SettingsProblem>();
    }

    @Override
    public Server getServer() {
        return servers.isEmpty() ? null : servers.get(0);
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public Proxy getProxy() {
        return proxies.isEmpty() ? null : proxies.get(0);
    }

    @Override
    public List<Proxy> getProxies() {
        return proxies;
    }

    @Override
    public List<SettingsProblem> getProblems() {
        return problems;
    }
}
