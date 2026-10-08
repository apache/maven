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
package org.apache.maven.cling.invoker.mvn.resident;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenContext;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.apache.maven.logging.SimpleBuildEventListener;
import org.apache.maven.logging.internal.DefaultOutputCapabilities;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentOutputCapabilitiesTest {
    @TempDir
    Path directory;

    @Test
    void reusedContainerRefreshesCapabilitiesButPreservesEarlierMaps() throws Exception {
        Files.createDirectories(directory.resolve(".mvn"));
        Files.writeString(
                directory.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId>"
                        + "<artifactId>output-capabilities</artifactId><version>1</version>"
                        + "<packaging>pom</packaging></project>");
        Path file = directory.resolve("build.log");
        try (ClassWorld world = new ClassWorld("plexus.core", getClass().getClassLoader());
                InspectingInvoker invoker = new InspectingInvoker(world)) {
            assertEquals(0, invoker.invoke(request(List.of("-l", file.toString(), "--color=always", "validate"))));
            OutputCapabilities component = invoker.component;
            Map<?, ?> fileMap = invoker.captured;
            assertEquals("FILE", invoker.captured.get("destination"));
            assertEquals("UTF-8", invoker.captured.get("encoding"));
            assertEquals(Destination.FILE, invoker.destination);
            assertEquals(Optional.of(StandardCharsets.UTF_8), invoker.encoding);
            assertEquals(Destination.UNKNOWN, component.getDestination());
            assertEquals(Optional.empty(), component.getEncoding());
            assertEquals("FILE", fileMap.get("destination"));
            assertEquals("UTF-8", fileMap.get("encoding"));
            assertTrue(Files.readString(file).contains("output-capabilities-marker"));

            assertEquals(0, invoker.invoke(request(List.of("validate"))));
            assertSame(component, invoker.component);
            assertNotSame(fileMap, invoker.captured);
            assertEquals(Destination.UNKNOWN, invoker.destination); // embedded byte stream
            assertTrue(invoker.encoding.isPresent());
            Map<?, ?> embeddedMap = invoker.captured;
            assertEquals("UNKNOWN", embeddedMap.get("destination"));
            assertEquals(invoker.encoding.get().name(), embeddedMap.get("encoding"));
            assertEquals(Optional.empty(), component.getEncoding());
            assertEquals("FILE", fileMap.get("destination"));
            assertEquals("UTF-8", fileMap.get("encoding"));

            invoker.fail = true;
            InvokerException.ExitException failure = assertThrows(
                    InvokerException.ExitException.class,
                    () -> invoker.invoke(request(List.of("-l", file.toString(), "validate"))));
            assertEquals(2, failure.getExitCode());
            assertSame(component, invoker.component);
            assertNotSame(fileMap, invoker.captured);
            assertEquals(Destination.FILE, invoker.destination);
            assertEquals(fileMap, invoker.captured);
            assertEquals("UNKNOWN", embeddedMap.get("destination"));
            assertTrue(embeddedMap.containsKey("encoding"));
            assertEquals(Destination.UNKNOWN, component.getDestination());
            assertEquals(Optional.empty(), component.getEncoding());
            assertEquals("FILE", fileMap.get("destination"));
            assertEquals("UTF-8", fileMap.get("encoding"));
        }
    }

    @Test
    void customBuildListenerDoesNotInheritTerminalMetadata() throws Exception {
        Files.createDirectories(directory.resolve(".mvn"));
        Files.writeString(
                directory.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId>"
                        + "<artifactId>output-capabilities</artifactId><version>1</version>"
                        + "<packaging>pom</packaging></project>");
        try (ClassWorld world = new ClassWorld("plexus.core", getClass().getClassLoader());
                InspectingInvoker invoker = new InspectingInvoker(world)) {
            invoker.customListener = true;
            assertEquals(0, invoker.invoke(request(List.of("validate"))));
            assertEquals(Destination.UNKNOWN, invoker.destination);
            assertEquals(Optional.empty(), invoker.encoding);
        }
    }

    private InvokerRequest request(List<String> arguments) throws Exception {
        return new MavenParser()
                .parseInvocation(ParserRequest.mvn(arguments, new JLineMessageBuilderFactory())
                        .cwd(directory)
                        .userHome(directory)
                        .stdOut(new ByteArrayOutputStream())
                        .stdErr(new ByteArrayOutputStream())
                        .embedded(true)
                        .build());
    }

    private static class InspectingInvoker extends ResidentMavenInvoker {
        private OutputCapabilities component;
        private Destination destination;
        private Optional<java.nio.charset.Charset> encoding;
        private Map<?, ?> captured;
        private boolean fail;
        private boolean customListener;

        InspectingInvoker(ClassWorld world) {
            super(ProtoLookup.builder().addMapping(ClassWorld.class, world).build(), null);
        }

        @Override
        protected MavenContext createContext(InvokerRequest request) {
            MavenContext context = super.createContext(request);
            if (customListener) {
                context.buildEventListener = new SimpleBuildEventListener(ignored -> {});
            }
            return context;
        }

        @Override
        protected int doExecute(MavenContext context, MavenExecutionRequest request) throws Exception {
            component = context.lookup.lookup(OutputCapabilities.class);
            destination = component.getDestination();
            encoding = component.getEncoding();
            request.setLocalRepositoryPath(context.cwd.resolve("repository").toFile());
            int result = super.doExecute(context, request);
            assertEquals(0, result);
            captured = (Map<?, ?>) request.getData().get("maven.logging.outputCapabilities");
            assertSame(((DefaultOutputCapabilities) component).asMap(), captured);
            assertEquals(destination.name(), captured.get("destination"));
            assertEquals(encoding.map(java.nio.charset.Charset::name).orElse(null), captured.get("encoding"));
            context.logger.info("output-capabilities-marker");
            if (fail) {
                throw new IllegalStateException("test invocation failure");
            }
            return result;
        }
    }
}
