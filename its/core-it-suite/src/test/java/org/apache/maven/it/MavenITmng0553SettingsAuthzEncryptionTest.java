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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-553">MNG-553</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng0553SettingsAuthzEncryptionTest extends AbstractMavenIntegrationTestCase {

    private File testDir;

    private Server server;

    private int port;

    public MavenITmng0553SettingsAuthzEncryptionTest() {
        super("[2.1.0,3.0-alpha-1),[3.0-alpha-3,4.0.0-beta-4]");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        testDir = extractResources("/mng-0553");

        Constraint constraint = new Constraint(__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"user"});
        userRealm.setUserStore(userStore);

        server = new Server(0);
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthMethod(__BASIC_AUTH);
        securityHandler.setConstraintMappings(new ConstraintMapping[] {constraintMapping});

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase(new File(testDir, "repo").getAbsolutePath());

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(securityHandler);
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        server.setHandler(handlerList);
        server.start();
        if (server.isFailed()) {
            fail("Couldn't bind the server socket to a free port!");
        }
        port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
        System.out.println("Bound server socket to the port " + port);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that the encrypted auth infos given in the settings.xml are decrypted.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitBasic() throws Exception {
        testDir = new File(testDir, "test-1");

        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@port@", Integer.toString(port));

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng0553");
        verifier.verifyArtifactNotPresent("org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar");
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
        verifier.setUserHomeDirectory(new File(testDir, "userhome").toPath());
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar");
    }

    /**
     * Test that the encrypted auth infos given in the settings.xml are decrypted when the master password resides
     * in an external file.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRelocation() throws Exception {
        testDir = new File(testDir, "test-2");

        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@port@", Integer.toString(port));
        // NOTE: The upper-case scheme name is essential part of the test
        String secUrl = "FILE://"
                + new File(testDir, "relocated-settings-security.xml").toURI().getRawPath();
        filterProps.put("@relocation@", secUrl);

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng0553");
        verifier.verifyArtifactNotPresent("org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar");

        // NOTE: The tilde ~ in the file name is essential part of the test
        verifier.filterFile("security-template.xml", "settings~security.xml", filterProps);
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);

        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        // NOTE: The selection of the Turkish language for the JVM locale is essential part of the test
        verifier.setEnvironmentVariable(
                "MAVEN_OPTS",
                "-Dsettings.security=" + new File(testDir, "settings~security.xml").getAbsolutePath()
                        + " -Duser.language=tr");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar");
    }

    /**
     * Test that the CLI supports generation of encrypted (master) passwords.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitEncryption() throws Exception {
        requiresMavenVersion("[2.1.0,3.0-alpha-1),[3.0-alpha-7,)");

        testDir = new File(testDir, "test-3");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setUserHomeDirectory(new File(testDir, "userhome").toPath());
        verifier.addCliArgument("--encrypt-master-password");
        verifier.addCliArgument("test");
        verifier.setLogFileName("log-emp.txt");
        verifier.addCliArgument("-e");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> log = verifier.loadLogLines();
        assertNotNull(findPassword(log));

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setUserHomeDirectory(new File(testDir, "userhome").toPath());
        verifier.addCliArgument("--encrypt-password");
        verifier.addCliArgument("testpass");
        verifier.setLogFileName("log-ep.txt");
        verifier.addCliArgument("-e");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        log = verifier.loadLogLines();
        assertNotNull(findPassword(log));
    }

    private String findPassword(List<String> log) {
        for (String line : log) {
            if (line.matches(".*\\{[A-Za-z0-9+/=]+\\}.*")) {
                return line;
            }
        }

        return null;
    }
}
