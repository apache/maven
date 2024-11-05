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
package org.apache.maven.it;

import java.io.File;
import java.util.Properties;

import org.apache.maven.settings.Proxy;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

class MavenITmng6401ProxyPortInterpolationTest extends AbstractMavenIntegrationTestCase {

    private Proxy proxy;

    private int port;

    protected MavenITmng6401ProxyPortInterpolationTest() {
        super("(4.0.0-alpha-7,)");
    }

    @Test
    public void testitEnvVars() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-6401-proxy-port-interpolation");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.setEnvironmentVariable("MAVEN_PROXY_ACTIVE_BOOLEAN", "true");
        verifier.setEnvironmentVariable("MAVEN_PROXY_HOST_STRING", "myproxy.host.net");
        verifier.setEnvironmentVariable("MAVEN_PROXY_PORT_INT", "18080");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/settings.properties");
        assertEquals("true", props.getProperty("settings.proxies.0.active"));
        assertEquals("myproxy.host.net", props.getProperty("settings.proxies.0.host"));
        assertEquals("18080", props.getProperty("settings.proxies.0.port"));
    }
}
