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
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4428">MNG-4428</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4428FollowHttpRedirectTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that redirects from HTTP to HTTP are getting followed.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitHttpToHttp() throws Exception {
        testit(true, true);
    }

    /**
     * Verify that redirects from HTTPS to HTTPS are getting followed.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitHttpsToHttps() throws Exception {
        testit(false, false);
    }

    /**
     * Verify that redirects using a relative location URL are getting followed. While a relative URL violates the
     * HTTP spec, popular HTTP clients do support them so we better do, too.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRelativeLocation() throws Exception {
        testit(true, true, true);
    }

    private void testit(boolean fromHttp, boolean toHttp) throws Exception {
        testit(fromHttp, toHttp, false);
    }

    private void testit(boolean fromHttp, boolean toHttp, boolean relativeLocation) throws Exception {
        File testDir = extractResources("/mng-4428");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        // NOTE: trust store cannot be reliably configured for the current JVM
        verifier.setForkJvm(true);

        // keytool -genkey -alias localhost -keypass key-passwd -keystore keystore -storepass store-passwd \
        //   -validity 4096 -dname "cn=localhost, ou=None, L=Seattle, ST=Washington, o=ExampleOrg, c=US" -keyalg RSA
        String storePath = new File(testDir, "keystore").getAbsolutePath();
        String storePwd = "store-passwd";
        String keyPwd = "key-passwd";

        Server server = new Server(0);
        addHttpsConnector(server, storePath, storePwd, keyPwd);
        Connector from = server.getConnectors()[fromHttp ? 0 : 1];
        Connector to = server.getConnectors()[toHttp ? 0 : 1];
        server.setHandler(
                new RedirectHandler(toHttp ? "http" : "https", relativeLocation ? null : (NetworkConnector) to));

        try {
            server.start();
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            verifier.setAutoclean(false);
            verifier.deleteArtifacts("org.apache.maven.its.mng4428");
            verifier.deleteDirectory("target");
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put("@protocol@", fromHttp ? "http" : "https");
            filterProps.put("@port@", Integer.toString(((NetworkConnector) from).getLocalPort()));
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("-X");
            verifier.addCliArgument("--settings");
            verifier.addCliArgument("settings.xml");
            verifier.setEnvironmentVariable(
                    "MAVEN_OPTS",
                    "-Djavax.net.ssl.trustStore=" + storePath + " -Djavax.net.ssl.trustStorePassword=" + storePwd);
            verifier.setLogFileName("log-" + getName().substring(6) + ".txt");
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
        } finally {
            server.stop();
            server.join();
        }

        List<String> cp = verifier.loadLines("target/classpath.txt");
        assertTrue(cp.contains("dep-0.1.jar"), cp.toString());
    }

    private void addHttpsConnector(Server server, String keyStorePath, String keyStorePassword, String keyPassword) {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setKeyManagerPassword(keyPassword);
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
        httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
        ServerConnector httpsConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration));
        server.addConnector(httpsConnector);
    }

    static class RedirectHandler extends Handler.Abstract {
        private final String protocol;

        private final NetworkConnector connector;

        RedirectHandler(String protocol, NetworkConnector connector) {
            this.protocol = protocol;
            this.connector = connector;
        }

        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            System.out.println("Handling " + request.getMethod() + " " + request.getHttpURI().toString());

            String uri = Request.getPathInContext(request);
            if (uri.startsWith("/repo/")) {
                String location = "/redirected/" + uri.substring(6);
                if (protocol != null && connector != null) {
                    location = protocol + "://localhost:" + connector.getLocalPort() + location;
                }
                if (uri.endsWith(".pom")) {
                    response.setStatus(302);
                } else {
                    response.setStatus(301);
                }
                response.getHeaders().put("Location", location);
            } else if (uri.endsWith(".pom")) {
                PrintWriter writer = new PrintWriter(Content.Sink.asOutputStream(response));
                writer.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">");
                writer.println("  <modelVersion>4.0.0</modelVersion>");
                writer.println("  <groupId>org.apache.maven.its.mng4428</groupId>");
                writer.println("  <artifactId>dep</artifactId>");
                writer.println("  <version>0.1</version>");
                writer.println("</project>");
                writer.flush();
                response.setStatus(200);
            } else if (uri.endsWith(".jar")) {
                PrintWriter writer = new PrintWriter(Content.Sink.asOutputStream(response));
                writer.println("empty");
                writer.flush();
                response.setStatus(200);
            } else {
                response.setStatus(404);
            }

            callback.succeeded();
            return true;
        }
    }
}
