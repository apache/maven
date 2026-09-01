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
package org.apache.maven.cling.transfer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.transfer.ChecksumFailureException;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that transfer listener output escapes control characters.
 */
class TransferListenerOutputTest {

    private static final char ESC = '\u001b';
    private static final String CURSOR_UP_AND_ERASE = ESC + "[1A" + ESC + "[2K";

    @Test
    void transferMessagesEscapeTerminalControlCharacters() throws TransferCancelledException {
        StringWriter out = new StringWriter();
        ConsoleMavenTransferListener listener =
                new ConsoleMavenTransferListener(new JLineMessageBuilderFactory(), new PrintWriter(out), true);

        TransferResource resource =
                new TransferResource("central", "http://repo/", "test/test-resource", new File(""), null);
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false); // no close handle
        TransferEvent event = new TransferEvent.Builder(session, resource)
                .setType(TransferEvent.EventType.CORRUPTED)
                .setException(new ChecksumFailureException("Checksum validation failed, expected 'deadbeef"
                        + CURSOR_UP_AND_ERASE + "' but is actually 'cafebabe'"))
                .build();

        listener.transferCorrupted(event);

        String printed = out.toString();
        assertTrue(printed.contains("Checksum validation failed"), "warning must still be printed");
        assertFalse(printed.indexOf(ESC) >= 0, "raw ESC must not reach the terminal");
        assertTrue(printed.contains("\\u001b"), "control characters must be escaped visibly");
    }

    @Test
    void escapesC0DelAndC1ButKeepsTabAndNewline() {
        assertEquals("plain-1.0.jar", AbstractMavenTransferListener.sanitize("plain-1.0.jar"));
        assertEquals("a\tb\nc", AbstractMavenTransferListener.sanitize("a\tb\nc"));
        assertEquals("a\\u001bb", AbstractMavenTransferListener.sanitize("a" + ESC + "b")); // ESC
        assertEquals("a\\u000db", AbstractMavenTransferListener.sanitize("a\rb")); // CR
        assertEquals("a\\u0007b", AbstractMavenTransferListener.sanitize("a\u0007b")); // BEL (OSC terminator)
        assertEquals("a\\u007fb", AbstractMavenTransferListener.sanitize("a\u007fb")); // DEL
        assertEquals("a\\u009bb", AbstractMavenTransferListener.sanitize("a\u009bb")); // CSI (C1)
        assertNull(AbstractMavenTransferListener.sanitize(null));
    }
}
