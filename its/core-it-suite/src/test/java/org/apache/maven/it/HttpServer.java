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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * An HTTP server that handles all requests on a given port from a specified source, optionally secured using BASIC auth
 * by providing a username and password. The source can either be a URL or a directory. When a request is made the
 * request is satisfied from the provided source.
 *
 * @author Jason van Zyl
 */
public class HttpServer {

    private final Server server;

    private final StreamSource source;

    private final String username;

    private final String password;

    public HttpServer(int port, String username, String password, StreamSource source) {
        this.username = username;
        this.password = password;
        this.source = source;
        this.server = server(port);
    }

    public void start() throws Exception {
        server.start();
        // server.join();
    }

    public boolean isFailed() {
        return server.isFailed();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void join() throws Exception {
        server.join();
    }

    public int port() {
        return ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }

    private Server server(int port) {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);
        Server server = new Server(threadPool);
        server.setConnectors(new Connector[] {new ServerConnector(server)});

        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        connector.setIdleTimeout(30_000L);
        connector.setPort(port);

        StreamSourceHandler handler = new StreamSourceHandler(source);

        if (username != null && password != null) {
            HashLoginService loginService = new HashLoginService("Test Server");
            UserStore userStore = new UserStore();
            userStore.addUser(username, new Password(password), new String[] {"user"});
            loginService.setUserStore(userStore);
            server.addBean(loginService);

            SecurityHandler.PathMapped security = new SecurityHandler.PathMapped();
            security.setAuthenticator(new BasicAuthenticator());
            security.setLoginService(loginService);
            security.put("/*", Constraint.from("auth", Constraint.Authorization.ANY_USER));
            security.setHandler(handler);
            server.setHandler(security);
        } else {
            server.setHandler(handler);
        }
        return server;
    }

    public static HttpServerBuilder builder() {
        return new HttpServerBuilder();
    }

    public static class HttpServerBuilder {

        private int port;

        private String username;

        private String password;

        private StreamSource source;

        public HttpServerBuilder port(int port) {
            this.port = port;
            return this;
        }

        public HttpServerBuilder username(String username) {
            this.username = username;
            return this;
        }

        public HttpServerBuilder password(String password) {
            this.password = password;
            return this;
        }

        public HttpServerBuilder source(final String source) {
            this.source = new StreamSource() {
                @Override
                public InputStream stream(String path) throws IOException {
                    return new URL(String.format("%s/%s", source, path)).openStream();
                }
            };
            return this;
        }

        public HttpServerBuilder source(final File source) {
            this.source = new StreamSource() {
                @Override
                public InputStream stream(String path) throws IOException {
                    return new FileInputStream(new File(source, path));
                }
            };
            return this;
        }

        public HttpServer build() {
            return new HttpServer(port, username, password, source);
        }
    }

    public interface StreamSource {
        InputStream stream(String path) throws IOException;
    }

    public static class StreamSourceHandler extends Handler.Abstract {

        private final StreamSource source;

        public StreamSourceHandler(StreamSource source) {
            this.source = source;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
            response.setStatus(200);
            String target = Request.getPathInContext(request);
            try (InputStream in = source.stream(target.substring(1));
                    OutputStream out = Content.Sink.asOutputStream(response)) {
                in.transferTo(out);
            }
            callback.succeeded();
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.builder() //
                .port(0) //
                .username("maven") //
                .password("secret") //
                .source(new File("/tmp/repo")) //
                .build();
        server.start();
    }
}
