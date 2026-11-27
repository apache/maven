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
package org.fusesource.jansi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.maven.jline.MessageUtils;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiTest {

    private Terminal originalTerminal;
    private boolean originalColorEnabled;
    private boolean originalAnsiEnabled;

    @BeforeEach
    void setUp() {
        originalTerminal = MessageUtils.getTerminal();
        originalColorEnabled = MessageUtils.isColorEnabled();
        originalAnsiEnabled = org.jline.jansi.Ansi.isEnabled();
        Ansi.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        Ansi.setEnabled(originalAnsiEnabled);
        MessageUtils.systemInstall(originalTerminal);
        MessageUtils.setColorEnabled(originalColorEnabled);
    }

    @Test
    void renderStripsMarkupWhenMessageUtilsColorDisabled() throws Exception {
        try (Terminal terminal = new DumbTerminal(
                "test", Terminal.TYPE_DUMB, InputStream.nullInputStream(), new ByteArrayOutputStream(), null)) {
            MessageUtils.systemInstall(terminal);
            MessageUtils.setColorEnabled(false);

            assertFalse(Ansi.isEnabled());
            assertEquals("hello", Ansi.ansi().render("@|red hello|@").toString());
            assertEquals(
                    "prefix bold text middle bg and fg suffix",
                    Ansi.ansi()
                            .render("prefix @|bold,red bold text|@ middle @|bg_blue,white bg and fg|@ suffix")
                            .toString());
            assertEquals(
                    "world",
                    Ansi.ansi()
                            .fg(Ansi.Color.RED)
                            .render("@|bold world|@")
                            .reset()
                            .toString());
        }
    }

    @Test
    void renderStripsMarkupWhenNoTerminalInstalled() {
        MessageUtils.systemInstall((Terminal) null);
        MessageUtils.setColorEnabled(true);

        assertFalse(Ansi.isEnabled());
        assertEquals("hello", Ansi.ansi().render("@|green hello|@").toString());
    }

    @Test
    void renderStripsMarkupWhenAnsiDisabled() throws Exception {
        try (Terminal terminal = new DumbTerminal(
                "test", Terminal.TYPE_DUMB, InputStream.nullInputStream(), new ByteArrayOutputStream(), null)) {
            MessageUtils.systemInstall(terminal);
            MessageUtils.setColorEnabled(true);
            Ansi.setEnabled(false);

            assertFalse(Ansi.isEnabled());
            assertEquals("hello", Ansi.ansi().render("@|yellow hello|@").toString());
        }
    }

    @Test
    void renderWithFormatArgsStripsMarkupWhenColorDisabled() {
        MessageUtils.systemInstall((Terminal) null);
        MessageUtils.setColorEnabled(false);

        assertEquals(
                "Error: file.xml (2)",
                Ansi.ansi().render("@|red Error|@: %s (%d)", "file.xml", 2).toString());
        assertEquals(
                "100% done",
                Ansi.ansi().a("100% ").render("@|green %s|@", "done").toString());
    }

    @Test
    void renderHandlesPlainAndEdgeCaseInputsWhenColorDisabled() {
        MessageUtils.systemInstall((Terminal) null);
        MessageUtils.setColorEnabled(false);

        assertEquals("plain text", Ansi.ansi().render("plain text").toString());
        assertEquals("spaced text", Ansi.ansi().render("@|red spaced text|@").toString());
        assertEquals(
                "ok",
                Ansi.ansi().render("@|faint,underline,fg_cyan,bg_default ok|@").toString());
        assertEquals(
                "ok x raw @|unclosed",
                Ansi.ansi().render("ok @|bold x|@ raw @|unclosed").toString());
        assertEquals(
                "ok @|nospace|@ @|red y|@",
                Ansi.ansi().render("ok @|nospace|@ @|red y|@").toString());
        assertThrows(IllegalArgumentException.class, () -> Ansi.ansi().render("@|unknown text|@"));
        assertThrows(IllegalArgumentException.class, () -> Ansi.ansi().render("@|@"));
        assertThrows(IllegalArgumentException.class, () -> Ansi.ansi().render("@|unknown %s|@", "text"));
    }

    @Test
    void renderEmitsAnsiSequencesWhenColorEnabled() throws Exception {
        try (Terminal terminal = new DumbTerminal(
                "test", Terminal.TYPE_DUMB, InputStream.nullInputStream(), new ByteArrayOutputStream(), null)) {
            MessageUtils.systemInstall(terminal);
            MessageUtils.setColorEnabled(true);
            Ansi.setEnabled(true);

            assertTrue(Ansi.isEnabled());
            String colored = Ansi.ansi().render("@|red hello|@").toString();
            assertTrue(colored.contains("\u001b["));
            assertTrue(colored.contains("hello"));

            String formatted = Ansi.ansi().render("@|bold %s|@ %d", "ok", 3).toString();
            assertTrue(formatted.contains("\u001b["));
            assertTrue(formatted.contains("ok"));
            assertTrue(formatted.contains(" 3"));
        }
    }
}
