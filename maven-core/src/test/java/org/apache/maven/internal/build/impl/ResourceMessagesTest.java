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
package org.apache.maven.internal.build.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.build.Severity;
import org.apache.maven.api.build.spi.Message;
import org.apache.maven.api.build.spi.Sink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceMessagesTest {
    @TempDir
    public Path temp;

    private TestBuildContext newBuildContext() throws IOException {
        return new TestBuildContext();
    }

    @Test
    public void testInputMessages() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        // initial message
        TestBuildContext context = newBuildContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        inputFile = metadata.getPath();
        DefaultInput input = metadata.process();
        input.addMessage(0, 0, "message", Severity.WARNING, null);
        context.commit();

        // the message is retained during no-change rebuild
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        List<Message> messages = context.getMessages(inputFile);
        assertEquals(1, messages.size());
        assertEquals("message", messages.get(0).getMessage());
        context.commit();

        // the message is retained during second no-change rebuild
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        messages = context.getMessages(inputFile);
        assertEquals(1, messages.size());
        assertEquals("message", messages.get(0).getMessage());
        context.commit();

        // new message
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        input = metadata.process();
        input.addMessage(0, 0, "newMessage", Severity.WARNING, null);
        context.commit();
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        messages = context.getMessages(inputFile);
        assertEquals(1, messages.size());
        assertEquals("newMessage", messages.get(0).getMessage());
        context.commit();

        // removed message
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        input = metadata.process();
        context.commit();
        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        assertNull(context.getMessages(inputFile));
        context.commit();
    }

    @Test
    public void testInputMessages_nullMessageText() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        // initial message
        TestBuildContext context = newBuildContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        inputFile = metadata.getPath();
        DefaultInput input = metadata.process();
        input.addMessage(0, 0, null, Severity.WARNING, null);
        context.commit();

        context = newBuildContext();
        metadata = context.registerInput(inputFile);
        List<Message> messages = context.getMessages(inputFile);
        assertEquals(1, messages.size());
        assertNull(messages.get(0).getMessage());
        context.commit();
    }

    @Test
    public void testExcludedInputMessageCleanup() throws Exception {
        Path inputFile = Files.createFile(temp.resolve("inputFile"));

        // initial message
        TestBuildContext context = newBuildContext();
        DefaultInputMetadata metadata = context.registerInput(inputFile);
        inputFile = metadata.getPath();
        DefaultInput input = metadata.process();
        input.addMessage(0, 0, "message", Severity.WARNING, null);
        context.commit();

        // input is removed from input set, make sure input messages are cleaned up
        final List<Object> cleared = new ArrayList<>();
        newBuildContext().commit(new Sink() {
            @Override
            public void messages(Path resource, boolean isNew, Collection<Message> messages) {
                assertTrue(messages.isEmpty());
            }

            public void clear(Path resource) {
                cleared.add(resource);
            }
        });

        assertEquals(1, cleared.size());
        assertEquals(inputFile, cleared.get(0));
    }

    private class TestBuildContext extends DefaultBuildContext {
        protected TestBuildContext() throws IOException {
            super(new FilesystemWorkspace(), temp.resolve("buildstate.ctx"), Collections.emptyMap(), null);
        }

        public void commit() throws IOException {
            super.commit(null);
        }

        public List<Message> getMessages(Path resource) {
            Collection<Message> messages = getState(resource).getResourceMessages(resource);
            return messages != null ? new ArrayList<>(messages) : null;
        }

        protected DefaultBuildContextState getState(Path source) {
            return isProcessedResource(source) ? this.state : this.oldState;
        }
    }
}
