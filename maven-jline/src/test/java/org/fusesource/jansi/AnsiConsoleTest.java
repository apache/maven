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
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that this JAnsi compatibility shim never installs anything of its own (see #13726, #13727).
 */
class AnsiConsoleTest {

    @AfterEach
    void uninstall() {
        if (AnsiConsole.isInstalled()) {
            AnsiConsole.systemUninstall();
        }
    }

    @Test
    void systemInstallLeavesSystemOutAlone() {
        PrintStream original = System.out;
        assertFalse(AnsiConsole.isInstalled());

        AnsiConsole.systemInstall();
        assertTrue(AnsiConsole.isInstalled());
        assertSame(original, System.out);

        AnsiConsole.systemUninstall();
        assertFalse(AnsiConsole.isInstalled());
        assertSame(original, System.out);
    }

    @Test
    void outWritesToWhicheverStreamIsCurrentlySystemOut() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured));
            AnsiConsole.out().print("hello");
            AnsiConsole.out().flush();
        } finally {
            System.setOut(original);
        }

        assertEquals("hello", captured.toString());
    }

    @Test
    void outReturnsTheSameInstanceEveryCall() {
        assertSame(AnsiConsole.out(), AnsiConsole.out());
    }

    @Test
    void getTerminalWidthDelegatesToMessageUtils() {
        assertEquals(org.apache.maven.jline.MessageUtils.getTerminalWidth(), AnsiConsole.getTerminalWidth());
    }

    @Test
    void renderHonoursColorSetting() {
        org.apache.maven.jline.MessageUtils.setColorEnabled(true);
        try {
            assertEquals(
                    "a \u001B[1;31mb\u001B[m c",
                    Ansi.ansi().render("a @|bold,red b|@ c").toString());
        } finally {
            org.apache.maven.jline.MessageUtils.setColorEnabled(false);
        }
        assertEquals("a b c", Ansi.ansi().render("a @|bold,red b|@ c").toString());
    }

    @Test
    void setModeOnTheSharedPrintStreamIsANoOp() {
        AnsiPrintStream out = AnsiConsole.out();
        assertDoesNotThrow(() -> out.setMode(AnsiMode.Force));
        assertEquals(AnsiMode.Default, out.getMode());
    }
}
