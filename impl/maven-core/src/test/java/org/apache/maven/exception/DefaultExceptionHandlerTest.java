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
package org.apache.maven.exception;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
class DefaultExceptionHandlerTest {
    /**
     * Running Maven under JDK7 may cause connection issues because IPv6 is used by default.
     * <p>
     * e.g running mvn site:run will cause Jetty to fail.
     * </p>
     * <p>
     * The resolution is to add -Djava.net.preferIPv4Stack=true to the command line as documented in
     * http://cwiki.apache.org/confluence/display/MAVEN/ConnectException
     * </p>
     */
    @Test
    void testJdk7ipv6() {
        ConnectException connEx = new ConnectException("Connection refused: connect");
        IOException ioEx = new IOException("Unable to establish loopback connection", connEx);
        MojoExecutionException mojoEx =
                new MojoExecutionException("Error executing Jetty: Unable to establish loopback connection", ioEx);

        ExceptionHandler exceptionHandler = new DefaultExceptionHandler();
        ExceptionSummary exceptionSummary = exceptionHandler.handleException(mojoEx);

        String expectedReference = "http://cwiki.apache.org/confluence/display/MAVEN/ConnectException";
        assertEquals(expectedReference, exceptionSummary.getReference());
    }

    @Test
    void testHandleExceptionAetherClassNotFound() {
        Throwable cause2 = new NoClassDefFoundError("org/sonatype/aether/RepositorySystem");
        Plugin plugin = new Plugin();
        Exception cause = new PluginContainerException(plugin, null, null, cause2);
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);
        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);
        Throwable exception = new PluginExecutionException(mojoExecution, null, cause);

        DefaultExceptionHandler handler = new DefaultExceptionHandler();
        ExceptionSummary summary = handler.handleException(exception);

        String expectedReference = "http://cwiki.apache.org/confluence/display/MAVEN/AetherClassNotFound";
        assertEquals(expectedReference, summary.getReference());
    }

    @Test
    void testHandleExceptionNoClassDefFoundErrorNull() {
        Throwable cause2 = new NoClassDefFoundError();
        Plugin plugin = new Plugin();
        Exception cause = new PluginContainerException(plugin, null, null, cause2);
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);
        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);
        Throwable exception = new PluginExecutionException(mojoExecution, null, cause);

        DefaultExceptionHandler handler = new DefaultExceptionHandler();
        ExceptionSummary summary = handler.handleException(exception);

        String expectedReference = "http://cwiki.apache.org/confluence/display/MAVEN/PluginContainerException";
        assertEquals(expectedReference, summary.getReference());
    }

    @Test
    void testHandleExceptionLoopInCause() {
        // Some broken exception that does return "this" as getCause
        AtomicReference<Throwable> causeRef = new AtomicReference<>(null);
        Exception cause2 = new RuntimeException("loop") {
            @Override
            public synchronized Throwable getCause() {
                return causeRef.get();
            }
        };
        causeRef.set(cause2);

        Plugin plugin = new Plugin();
        Exception cause = new PluginContainerException(plugin, null, null, cause2);
        cause2.initCause(cause);
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);
        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);
        Throwable exception = new PluginExecutionException(mojoExecution, null, cause);

        DefaultExceptionHandler handler = new DefaultExceptionHandler();
        ExceptionSummary summary = handler.handleException(exception);

        String expectedReference = "http://cwiki.apache.org/confluence/display/MAVEN/PluginContainerException";
        assertEquals(expectedReference, summary.getReference());
    }

    @Test
    void testHandleExceptionSelfReferencing() {
        RuntimeException boom3 = new RuntimeException("BOOM3");
        RuntimeException boom2 = new RuntimeException("BOOM2", boom3);
        RuntimeException boom1 = new RuntimeException("BOOM1", boom2);
        boom3.initCause(boom1);

        DefaultExceptionHandler handler = new DefaultExceptionHandler();
        ExceptionSummary summary = handler.handleException(boom1);

        assertEquals("BOOM1: BOOM2: BOOM3: [CIRCULAR REFERENCE]", summary.getMessage());
        assertEquals("", summary.getReference());
        assertEquals(0, summary.getChildren().size());
        assertEquals(boom1, summary.getException());
    }
}
