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
package org.apache.maven.cling.invoker.mvn;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.impl.DefaultMessageBuilder;
import org.apache.maven.impl.DefaultMessageBuilderFactory;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_OUT)
@ResourceLock(SYSTEM_PROPERTIES)
class MavenDiDiagnosticsTest extends MavenInvokerTestSupport {
    @Test
    void embeddedInvocationsStyleEarlyDiagnosticsWithTheirOwnFactory(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>di-diagnostics</artifactId>
                  <version>1</version>
                </project>
                """);
        Files.createDirectories(directory.resolve(".mvn"));
        Path userHome = Files.createDirectory(directory.resolve("home"));
        String property = "maven.di.debug";
        String previous = System.getProperty(property);
        try (ClassWorld world = createClassWorld();
                Invoker invoker = createInvoker(world)) {
            System.setProperty(property, "true");
            for (String tag : List.of("first:", "second:")) {
                var factory = new DefaultMessageBuilderFactory() {
                    @Override
                    public boolean isColorEnabled() {
                        return true;
                    }

                    @Override
                    public MessageBuilder builder() {
                        return new DefaultMessageBuilder() {
                            @Override
                            public MessageBuilder project(Object message) {
                                if (message instanceof String text && text.contains("[loader=")) {
                                    return a(tag).a(text);
                                }
                                return super.project(message);
                            }
                        };
                    }
                };
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                int code = invoker.invoke(createParser()
                        .parseInvocation(ParserRequest.mvn(List.of("-o", "validate"), factory)
                                .cwd(directory)
                                .userHome(userHome)
                                .stdOut(stdout)
                                .stdErr(stderr)
                                .embedded(true)
                                .build()));
                String dump = stdout.toString(StandardCharsets.UTF_8);
                assertEquals(0, code, dump + stderr.toString(StandardCharsets.UTF_8));
                int discovery = dump.indexOf("(discovery)");
                assertTrue(discovery >= 0, dump);
                int end = dump.indexOf("} Maven DI native registry ", discovery);
                assertTrue(end > discovery, dump);
                assertTrue(dump.substring(discovery, end).contains("key: " + tag + "org.apache.maven"), dump);
                assertFalse(dump.contains("key: " + (tag.equals("first:") ? "second:" : "first:")), dump);
            }
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }

    @Override
    protected Invoker createInvoker(ClassWorld classWorld) {
        return new MavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build(), null);
    }

    @Override
    protected Parser createParser() {
        return new MavenParser();
    }
}
