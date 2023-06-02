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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple HTTP proxy that only understands the CONNECT method to check HTTPS tunneling.
 *
 * @author Benjamin Bentmann
 */
public class TunnelingProxyServer implements Runnable {

    private int port;

    private volatile ServerSocket server;

    private String targetHost;

    private int targetPort;

    private String connectFilter;

    public TunnelingProxyServer(int port, String targetHost, int targetPort, String connectFilter) {
        this.port = port;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.connectFilter = connectFilter;
    }

    public int getPort() {
        return (server != null) ? server.getLocalPort() : port;
    }

    public void start() throws IOException {
        server = new ServerSocket(port, 4);
        new Thread(this).start();
    }

    public void stop() throws IOException {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    public void run() {
        try {
            while (true) {
                new ClientHandler(server.accept()).start();
            }
        } catch (Exception e) {
            // closed
        }
    }

    class ClientHandler extends Thread {

        private Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        public void run() {
            try {
                PushbackInputStream is = new PushbackInputStream(client.getInputStream());

                String dest = null;

                while (true) {
                    String line = readLine(is);
                    if (line == null || line.length() <= 0) {
                        break;
                    }
                    Matcher m = Pattern.compile("CONNECT +([^:]+:[0-9]+) +.*").matcher(line);
                    if (m.matches()) {
                        dest = m.group(1);
                    }
                }

                OutputStream os = client.getOutputStream();

                if (dest == null || (connectFilter != null && !dest.matches(connectFilter))) {
                    os.write(("HTTP/1.0 400 Bad request for " + dest + "\r\n\r\n").getBytes("UTF-8"));
                    return;
                }

                os.write("HTTP/1.0 200 Connection established\r\n\r\n".getBytes("UTF-8"));

                Socket server = new Socket(targetHost, targetPort);

                Thread t1 = new StreamPumper(is, server.getOutputStream());
                t1.start();
                Thread t2 = new StreamPumper(server.getInputStream(), os);
                t2.start();
                t1.join();
                t2.join();

                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String readLine(PushbackInputStream is) throws IOException {
            StringBuilder buffer = new StringBuilder(1024);

            while (true) {
                int b = is.read();
                if (b < 0) {
                    return null;
                } else if (b == '\n') {
                    break;
                } else if (b == '\r') {
                    b = is.read();
                    if (b != '\n') {
                        is.unread(b);
                    }
                    break;
                } else {
                    buffer.append((char) b);
                }
            }

            return buffer.toString();
        }
    }

    static class StreamPumper extends Thread {

        private final InputStream is;

        private final OutputStream os;

        public StreamPumper(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                for (byte[] buffer = new byte[1024 * 8]; ; ) {
                    int n = is.read(buffer);
                    if (n < 0) {
                        break;
                    }
                    os.write(buffer, 0, n);
                }
            } catch (IOException e) {
                // closed
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
