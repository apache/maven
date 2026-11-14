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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Switches on ANSI escape processing ({@code ENABLE_VIRTUAL_TERMINAL_PROCESSING}) for the Windows console, so that the
 * old console host (conhost, which runs {@code cmd.exe} outside Windows Terminal) renders colors instead of printing
 * escape codes.
 * <p>
 * Calls {@code kernel32} through the Java 22 Foreign Function and Memory API. Maven 3 compiles for Java 8, so the API
 * is reached by reflection. Nothing happens on older Java, or when native access is not enabled for this code
 * ({@code --enable-native-access=ALL-UNNAMED}, which {@code mvn.cmd} passes on Java 22 and later), so the JDK never
 * prints its restricted method warning.
 */
final class WindowsConsole {

    private static final int STD_OUTPUT_HANDLE = -11;
    private static final int STD_ERROR_HANDLE = -12;
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private static Boolean enabled;

    private WindowsConsole() {}

    /**
     * Enables virtual terminal processing on standard output and standard error, once per JVM.
     *
     * @return {@code true} if standard output now processes ANSI escape codes
     */
    static synchronized boolean enableVirtualTerminal() {
        if (enabled == null) {
            enabled = tryEnable();
        }
        return enabled;
    }

    private static boolean tryEnable() {
        if (!System.getProperty("os.name", "").startsWith("Windows") || !Ffm.available()) {
            return false;
        }
        try {
            Ffm kernel32 = Ffm.library("kernel32");
            MethodHandle getStdHandle = kernel32.downcall("GetStdHandle", Ffm.ADDRESS, Ffm.INT);
            MethodHandle getConsoleMode = kernel32.downcall("GetConsoleMode", Ffm.INT, Ffm.ADDRESS, Ffm.ADDRESS);
            MethodHandle setConsoleMode = kernel32.downcall("SetConsoleMode", Ffm.INT, Ffm.ADDRESS, Ffm.INT);
            boolean out = enable(kernel32, getStdHandle, getConsoleMode, setConsoleMode, STD_OUTPUT_HANDLE);
            enable(kernel32, getStdHandle, getConsoleMode, setConsoleMode, STD_ERROR_HANDLE);
            return out;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean enable(
            Ffm kernel32, MethodHandle getStdHandle, MethodHandle getConsoleMode, MethodHandle setConsoleMode, int std)
            throws Throwable {
        Object handle = getStdHandle.invokeWithArguments(std);
        Object modeCell = kernel32.allocateInt();
        if ((int) getConsoleMode.invokeWithArguments(handle, modeCell) == 0) {
            return false; // not a console (redirected)
        }
        int mode = kernel32.getInt(modeCell);
        if ((mode & ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
            return true;
        }
        // leave the console as we found it; registered first, so a JVM already shutting down changes nothing
        Thread restore = new Thread(() -> {
            try {
                setConsoleMode.invokeWithArguments(handle, mode);
            } catch (Throwable ignored) {
                // best effort
            }
        });
        Runtime.getRuntime().addShutdownHook(restore);
        if ((int) setConsoleMode.invokeWithArguments(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING) == 0) {
            Runtime.getRuntime().removeShutdownHook(restore);
            return false; // console host without VT support (before Windows 10)
        }
        return true;
    }

    /**
     * The few pieces of {@code java.lang.foreign} this class needs, reached by reflection.
     */
    static final class Ffm {
        static final String INT = "JAVA_INT";
        static final String ADDRESS = "ADDRESS";
        static final String LONG = "JAVA_LONG";

        private final Object lookup;

        private Ffm(Object lookup) {
            this.lookup = lookup;
        }

        /**
         * Returns {@code true} on Java 22 or later when this class may call native code without a warning.
         */
        static boolean available() {
            if (javaFeatureVersion() < 22) {
                return false; // java.lang.foreign is a preview API before 22
            }
            try {
                Object module = Class.class.getMethod("getModule").invoke(Ffm.class);
                Method enabled = module.getClass().getMethod("isNativeAccessEnabled"); // Java 22+
                return (Boolean) enabled.invoke(module);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        static int javaFeatureVersion() {
            String spec = System.getProperty("java.specification.version", "");
            try {
                return spec.startsWith("1.") ? 8 : Integer.parseInt(spec);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        static Ffm library(String name) throws ReflectiveOperationException {
            Object arena = cls("Arena").getMethod("global").invoke(null);
            Object lookup = cls("SymbolLookup")
                    .getMethod("libraryLookup", String.class, cls("Arena"))
                    .invoke(null, name, arena);
            return new Ffm(lookup);
        }

        static Ffm defaultLibrary() throws ReflectiveOperationException {
            return new Ffm(cls("Linker").getMethod("defaultLookup").invoke(linker()));
        }

        MethodHandle downcall(String symbol, String result, String... args) throws ReflectiveOperationException {
            Optional<?> address = (Optional<?>)
                    cls("SymbolLookup").getMethod("find", String.class).invoke(lookup, symbol);
            Object[] argLayouts = (Object[]) Array.newInstance(cls("MemoryLayout"), args.length);
            for (int i = 0; i < args.length; i++) {
                argLayouts[i] = layout(args[i]);
            }
            Object descriptor = cls("FunctionDescriptor")
                    .getMethod("of", cls("MemoryLayout"), argLayouts.getClass())
                    .invoke(null, layout(result), argLayouts);
            Object noOptions = Array.newInstance(cls("Linker$Option"), 0);
            return (MethodHandle) cls("Linker")
                    .getMethod("downcallHandle", cls("MemorySegment"), cls("FunctionDescriptor"), noOptions.getClass())
                    .invoke(
                            linker(),
                            address.orElseThrow(() -> new NoSuchMethodException(symbol)),
                            descriptor,
                            noOptions);
        }

        Object allocateInt() throws ReflectiveOperationException {
            Object arena = cls("Arena").getMethod("global").invoke(null);
            return cls("SegmentAllocator")
                    .getMethod("allocate", cls("MemoryLayout"))
                    .invoke(arena, layout(INT));
        }

        int getInt(Object segment) throws ReflectiveOperationException {
            return (Integer) cls("MemorySegment")
                    .getMethod("get", cls("ValueLayout$OfInt"), long.class)
                    .invoke(segment, layout(INT), 0L);
        }

        private static Object linker() throws ReflectiveOperationException {
            return cls("Linker").getMethod("nativeLinker").invoke(null);
        }

        private static Object layout(String name) throws ReflectiveOperationException {
            return cls("ValueLayout").getField(name).get(null);
        }

        private static Class<?> cls(String simpleName) throws ClassNotFoundException {
            return Class.forName("java.lang.foreign." + simpleName);
        }
    }
}
