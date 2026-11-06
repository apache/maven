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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Credentials of a {@code <server>} in settings are sent only to the origins the operator declared for its id:
 * the repositories and mirrors with that id in settings or on the command line, and the server's own
 * {@code <repositoryOrigins>}. See <a href="https://github.com/apache/maven/issues/13265">GH-13265</a>.
 * <p>
 * Two password-protected repositories serve the same parent POM, on two ports of {@code localhost} and so on two
 * origins. The project declares a repository with the server's id at one of them; whether the build can resolve
 * the parent shows whether the credentials were sent.
 */
class MavenITgh13265ServerRepositoryOriginsTest extends AbstractMavenIntegrationTestCase {

    private static final String GROUP_ID = "org.apache.maven.its.gh13265";

    private Path testDir;

    private Server declaredServer;

    private Server otherServer;

    @BeforeEach
    void setUp() throws Exception {
        testDir = extractResources("gh-13265-repository-origins");
        declaredServer = startRepository(testDir.resolve("repo"));
        otherServer = startRepository(testDir.resolve("repo"));
    }

    @AfterEach
    void tearDown() throws Exception {
        for (Server server : new Server[] {declaredServer, otherServer}) {
            if (server != null) {
                server.stop();
                server.join();
            }
        }
    }

    /**
     * The repository is at the origin the server declares: credentials are sent.
     */
    @Test
    void credentialsSentToDeclaredOrigin() throws Exception {
        Verifier verifier = build("declared", originsOf(declaredServer), url(declaredServer), null);
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog("Not using credentials of server 'it-repo'");
    }

    /**
     * {@code strict} scope still serves a declared origin. 4.0.0-rc-7 refused it because it did not read
     * {@code <repositoryOrigins>}.
     */
    @Test
    void credentialsSentToDeclaredOriginInStrictScope() throws Exception {
        Verifier verifier = build("declared-strict", originsOf(declaredServer), url(declaredServer), "strict");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog("Not using credentials of server 'it-repo'");
    }

    /**
     * The repository has the server's id but is at another origin: the credentials are not sent. This is the
     * property the feature exists for.
     */
    @Test
    void credentialsNotSentToOtherOrigin() throws Exception {
        Verifier verifier = build("other", originsOf(declaredServer), url(otherServer), null);
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("Not using credentials of server 'it-repo' for repository " + url(otherServer));
        verifier.verifyTextInLog("the origins declared for this id are");
    }

    @Test
    void credentialsNotSentToOtherOriginInStrictScope() throws Exception {
        Verifier verifier = build("other-strict", originsOf(declaredServer), url(otherServer), "strict");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("Not using credentials of server 'it-repo' for repository " + url(otherServer));
    }

    /**
     * Without {@code <repositoryOrigins>} and with no repository or mirror of that id in settings, the default
     * {@code origin} scope keeps the id-only behaviour of earlier Maven versions, with a warning.
     */
    @Test
    void credentialsSentWithWarningWithoutDeclaredOrigins() throws Exception {
        Verifier verifier = build("undeclared", "", url(otherServer), null);
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Using credentials of server 'it-repo' for repository " + url(otherServer)
                + ", although no repository or mirror with this id is declared");
    }

    @Test
    void credentialsNotSentWithoutDeclaredOriginsInStrictScope() throws Exception {
        Verifier verifier = build("undeclared-strict", "", url(otherServer), "strict");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("no repository or mirror with this id is declared in settings");
    }

    /**
     * A server alias inherits the origins of the server it is declared on.
     */
    @Test
    void aliasInheritsDeclaredOrigins() throws Exception {
        String extra = "      <aliases><alias>it-alias</alias></aliases>\n" + originsOf(declaredServer);
        Verifier declared = build("alias-declared", extra, url(declaredServer), "strict", "it-alias");
        declared.execute();
        declared.verifyErrorFreeLog();

        Verifier other = build("alias-other", extra, url(otherServer), "strict", "it-alias");
        assertThrows(VerificationException.class, other::execute);
        other.verifyTextInLog("Not using credentials of server 'it-alias' for repository " + url(otherServer));
    }

    /**
     * A settings element the model does not know is skipped; Maven says so at WARNING without {@code -e}, and keeps
     * the stack trace for {@code -e}.
     */
    @Test
    void unknownServerElementIsReportedWithoutErrorsSwitch() throws Exception {
        String extra = "      <repositoryOrigin>" + origin(declaredServer) + "</repositoryOrigin>";
        Verifier verifier = build("unknown-element", extra, url(declaredServer), null, "it-repo", false);
        verifier.execute();
        verifier.verifyTextInLog("[WARNING] Unable to read settings: Unrecognised tag: ");
        verifier.verifyTextInLog("repositoryOrigin' @ ");
        verifier.verifyTextNotInLog("at org.apache.maven.impl.XmlSettingsParser.parse");
    }

    private Verifier build(String name, String serverExtra, String repositoryUrl, String credentialScope)
            throws Exception {
        return build(name, serverExtra, repositoryUrl, credentialScope, "it-repo");
    }

    private Verifier build(
            String name, String serverExtra, String repositoryUrl, String credentialScope, String repositoryId)
            throws Exception {
        return build(name, serverExtra, repositoryUrl, credentialScope, repositoryId, true);
    }

    private Verifier build(
            String name,
            String serverExtra,
            String repositoryUrl,
            String credentialScope,
            String repositoryId,
            boolean showErrors)
            throws Exception {
        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@serverExtra@", serverExtra);
        filterProps.put("@repositoryId@", repositoryId);
        filterProps.put("@repositoryUrl@", repositoryUrl);

        Verifier verifier;
        if (showErrors) {
            verifier = newVerifier(testDir.resolve("project"));
        } else {
            // the default arguments include --errors; keep only --batch-mode and the suite's install settings
            verifier = new Verifier(testDir.resolve("project"), List.of("--batch-mode"));
            String settingsDir = System.getProperty("maven.it.global-settings.dir", "");
            Path installSettings = settingsDir.isEmpty()
                    ? Path.of("target/test-classes/settings-remote.xml")
                    : Path.of(settingsDir).resolve("settings-remote.xml");
            verifier.addCliArgument("--install-settings");
            verifier.addCliArgument(installSettings.toAbsolutePath().toString());
        }
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-" + name + ".txt");
        verifier.deleteArtifacts(GROUP_ID);
        verifier.filterFile("settings-template.xml", "settings-" + name + ".xml", filterProps);
        verifier.filterFile("pom-template.xml", "pom.xml", filterProps);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings-" + name + ".xml");
        if (credentialScope != null) {
            verifier.addCliArgument("-Dmaven.repository.credentialScope=" + credentialScope);
        }
        verifier.addCliArgument("validate");
        return verifier;
    }

    private static String originsOf(Server server) {
        return "      <repositoryOrigins>\n        <repositoryOrigin>" + origin(server)
                + "</repositoryOrigin>\n      </repositoryOrigins>";
    }

    private static String origin(Server server) {
        return "http://localhost:" + ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }

    private static String url(Server server) {
        return origin(server) + "/repo";
    }

    private static Server startRepository(Path repoDir) throws Exception {
        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"user"});
        userRealm.setUserStore(userStore);

        Server server = new Server(0);
        SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.put("/*", Constraint.from("auth", Constraint.Authorization.SPECIFIC_ROLE, "user"));

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setBaseResource(ResourceFactory.of(server).newResource(repoDir.getParent()));

        Handler.Sequence handlerList = new Handler.Sequence();
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        securityHandler.setHandler(handlerList);
        server.setHandler(securityHandler);
        server.start();
        if (server.isFailed()) {
            throw new IllegalStateException("Couldn't bind the server socket to a free port");
        }
        return server;
    }
}
