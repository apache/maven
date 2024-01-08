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
package org.apache.maven.utils;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * OS support
 */
public class Os {

    /**
     * The OS Name.
     */
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    /**
     * The OA architecture.
     */
    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    /**
     * The OS version.
     */
    public static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.ENGLISH);

    /**
     * OS Family
     */
    public static final String OS_FAMILY;

    /**
     * Boolean indicating if the running OS is a Windows system.
     */
    public static final boolean IS_WINDOWS;

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_WINDOWS = "windows";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_WIN9X = "win9x";

    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_NT = "winnt";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_OS2 = "os/2";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_NETWARE = "netware";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_DOS = "dos";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_MAC = "mac";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_TANDEM = "tandem";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_UNIX = "unix";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_OPENVMS = "openvms";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_ZOS = "z/os";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_OS390 = "os/390";

    /**
     * OS family that can be tested for. {@value}
     */
    private static final String FAMILY_OS400 = "os/400";

    /**
     * OpenJDK is reported to call MacOS X "Darwin"
     *
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=44889">bugzilla issue</a>
     * @see <a href="https://issues.apache.org/jira/browse/HADOOP-3318">HADOOP-3318</a>
     */
    private static final String DARWIN = "darwin";

    /**
     * The path separator.
     */
    private static final String PATH_SEP = System.getProperty("path.separator");

    static {
        // Those two public constants are initialized here, as they need all the private constants
        // above to be initialized first, but the code style imposes the public constants to be
        // defined above the private ones...
        OS_FAMILY = getOsFamily();
        IS_WINDOWS = isFamily(FAMILY_WINDOWS);
    }

    private Os() {}

    /**
     * Determines if the OS on which Maven is executing matches the
     * given OS family.
     *
     * @param family the family to check for
     * @return true if the OS matches
     *
     */
    public static boolean isFamily(String family) {
        // windows probing logic relies on the word 'windows' in the OS
        boolean isWindows = OS_NAME.contains(FAMILY_WINDOWS);
        boolean is9x = false;
        boolean isNT = false;
        if (isWindows) {
            // there are only four 9x platforms that we look for
            is9x = (OS_NAME.contains("95")
                    || OS_NAME.contains("98")
                    || OS_NAME.contains("me")
                    // wince isn't really 9x, but crippled enough to
                    // be a muchness. Maven doesnt run on CE, anyway.
                    || OS_NAME.contains("ce"));
            isNT = !is9x;
        }
        switch (family) {
            case FAMILY_WINDOWS:
                return isWindows;
            case FAMILY_WIN9X:
                return isWindows && is9x;
            case FAMILY_NT:
                return isWindows && isNT;
            case FAMILY_OS2:
                return OS_NAME.contains(FAMILY_OS2);
            case FAMILY_NETWARE:
                return OS_NAME.contains(FAMILY_NETWARE);
            case FAMILY_DOS:
                return PATH_SEP.equals(";") && !isFamily(FAMILY_NETWARE) && !isWindows;
            case FAMILY_MAC:
                return OS_NAME.contains(FAMILY_MAC) || OS_NAME.contains(DARWIN);
            case FAMILY_TANDEM:
                return OS_NAME.contains("nonstop_kernel");
            case FAMILY_UNIX:
                return PATH_SEP.equals(":")
                        && !isFamily(FAMILY_OPENVMS)
                        && (!isFamily(FAMILY_MAC) || OS_NAME.endsWith("x"));
            case FAMILY_ZOS:
                return OS_NAME.contains(FAMILY_ZOS) || OS_NAME.contains(FAMILY_OS390);
            case FAMILY_OS400:
                return OS_NAME.contains(FAMILY_OS400);
            case FAMILY_OPENVMS:
                return OS_NAME.contains(FAMILY_OPENVMS);
            default:
                return OS_NAME.contains(family.toLowerCase(Locale.US));
        }
    }

    /**
     * Helper method to determine the current OS family.
     *
     * @return name of current OS family.
     */
    private static String getOsFamily() {
        return Stream.of(
                        FAMILY_DOS,
                        FAMILY_MAC,
                        FAMILY_NETWARE,
                        FAMILY_NT,
                        FAMILY_OPENVMS,
                        FAMILY_OS2,
                        FAMILY_OS400,
                        FAMILY_TANDEM,
                        FAMILY_UNIX,
                        FAMILY_WIN9X,
                        FAMILY_WINDOWS,
                        FAMILY_ZOS)
                .filter(Os::isFamily)
                .findFirst()
                .orElse(null);
    }
}
