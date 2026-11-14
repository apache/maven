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
package org.apache.maven.jline;

import java.lang.invoke.MethodHandle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class WindowsConsoleTest {

    @Test
    void doesNothingOutsideWindows() {
        assumeFalse(System.getProperty("os.name", "").startsWith("Windows"));
        assertFalse(WindowsConsole.enableVirtualTerminal());
    }

    /**
     * Exercises the reflective downcall and memory access with the C library, so the code path kernel32 uses is
     * tested on every platform. Runs only on Java 22+ with {@code --enable-native-access=ALL-UNNAMED}.
     */
    @Test
    void callsNativeCodeThroughReflection() throws Throwable {
        assumeTrue(WindowsConsole.Ffm.available(), "needs Java 22+ with native access enabled");
        assumeFalse(System.getProperty("os.name", "").startsWith("Windows"));
        WindowsConsole.Ffm libc = WindowsConsole.Ffm.defaultLibrary();

        MethodHandle abs = libc.downcall("abs", WindowsConsole.Ffm.INT, WindowsConsole.Ffm.INT);
        assertEquals(42, (int) abs.invokeWithArguments(-42));

        Object cell = libc.allocateInt();
        MethodHandle memset = libc.downcall(
                "memset",
                WindowsConsole.Ffm.ADDRESS,
                WindowsConsole.Ffm.ADDRESS,
                WindowsConsole.Ffm.INT,
                WindowsConsole.Ffm.LONG);
        memset.invokeWithArguments(cell, 0x01, 4L);
        assertEquals(0x01010101, libc.getInt(cell));
    }

    /**
     * Resolves the three kernel32 functions on Windows. Under CI standard output is redirected, so the console mode
     * itself cannot be changed here.
     */
    @Test
    void resolvesKernel32OnWindows() throws Throwable {
        assumeTrue(System.getProperty("os.name", "").startsWith("Windows"));
        assumeTrue(WindowsConsole.Ffm.available(), "needs Java 22+ with native access enabled");
        WindowsConsole.Ffm kernel32 = WindowsConsole.Ffm.library("kernel32");
        MethodHandle getStdHandle =
                kernel32.downcall("GetStdHandle", WindowsConsole.Ffm.ADDRESS, WindowsConsole.Ffm.INT);
        kernel32.downcall(
                "GetConsoleMode", WindowsConsole.Ffm.INT, WindowsConsole.Ffm.ADDRESS, WindowsConsole.Ffm.ADDRESS);
        kernel32.downcall("SetConsoleMode", WindowsConsole.Ffm.INT, WindowsConsole.Ffm.ADDRESS, WindowsConsole.Ffm.INT);
        getStdHandle.invokeWithArguments(-11);
    }
}
