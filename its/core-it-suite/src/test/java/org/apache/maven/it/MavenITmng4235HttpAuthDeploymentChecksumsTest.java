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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.maven.it.utils.DeployedResource;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4235">MNG-4235</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4235HttpAuthDeploymentChecksumsTest extends AbstractMavenIntegrationTestCase {
    private File testDir;

    private Server server;

    private int port;

    private final RepoHandler repoHandler = new RepoHandler();

    @BeforeEach
    protected void setUp() throws Exception {
        testDir = extractResources("/mng-4235");

        repoHandler.setBasePath(testDir.toPath());

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testpass"), new String[] {"deployer"});
        userRealm.setUserStore(userStore);

        SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.put("/*", Constraint.from("auth", Constraint.Authorization.ANY_USER));

        server = new Server(0);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(ResourceFactory.of(server).newResource(testDir.toPath()));

        Handler.Sequence handlerList = new Handler.Sequence();
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(resourceHandler);
        handlerList.addHandler(new DefaultHandler());

        securityHandler.setHandler(handlerList);
        server.setHandler(securityHandler);
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
     * Test the creation of proper checksums during deployment to a secured HTTP repo. The pitfall with HTTP auth is
     * that it might require double submission of the data, first during an initial PUT without credentials and second
     * during a retried PUT with credentials in response to the auth challenge by the server. The checksum must
     * nevertheless only be calculated on the non-doubled data stream.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@port@", Integer.toString(port));

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.filterFile("pom-template.xml", "pom.xml", filterProps);
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4235");
        verifier.deleteDirectory("repo");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.jar", ".sha1", "SHA-1");
        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.jar", ".md5", "MD5");

        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.pom", ".sha1", "SHA-1");
        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.pom", ".md5", "MD5");

        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/maven-metadata.xml", ".sha1", "SHA-1");
        assertHash(verifier, "repo/org/apache/maven/its/mng4235/test/maven-metadata.xml", ".md5", "MD5");

        for (DeployedResource deployedResource : repoHandler.deployedResources) {
            if (StringUtils.equalsIgnoreCase("chunked", deployedResource.transferEncoding)) {
                fail("deployedResource " + deployedResource
                        + " use chunked transfert encoding some http server doesn't support that");
            }
        }
    }

    private void assertHash(Verifier verifier, String dataFile, String hashExt, String algo) throws Exception {
        String actualHash = ItUtils.calcHash(new File(verifier.getBasedir(), dataFile), algo);

        String expectedHash = verifier.loadLines(dataFile + hashExt).get(0).trim();

        assertTrue(expectedHash.equalsIgnoreCase(actualHash), "expected=" + expectedHash + ", actual=" + actualHash);
    }

    private static class RepoHandler extends Handler.Abstract {
        private final Deque<DeployedResource> deployedResources = new ConcurrentLinkedDeque<>();
        private Path basePath;

        void setBasePath(Path basePath) {
            this.basePath = basePath;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            System.out.println(request.getMethod() + " " + Request.getPathInContext(request));

            if ("PUT".equals(request.getMethod())) {
                Path resource = basePath.resolve(Request.getPathInContext(request).substring(1));

                // NOTE: This can get called concurrently but File.mkdirs() isn't thread-safe in all JREs
                File dir = resource.getParent().toFile();
                for (int i = 0; i < 10 && !dir.exists(); i++) {
                    dir.mkdirs();
                }

                Files.copy(Request.asInputStream(request), resource, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = Request.getPathInContext(request);
                deployedResource.transferEncoding = request.getHeaders().get("Transfer-Encoding");
                deployedResource.contentLength = request.getHeaders().get("Content-Length");

                deployedResources.add(deployedResource);

                response.setStatus(204);

                callback.succeeded();
                return true;
            }
            return false;
        }
    }
}
