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
package org.apache.maven.toolchain.discovery;

import java.util.Set;

import org.codehaus.plexus.util.Os;

public abstract class JavaHomeFinder {

    static final boolean IS_WINDOWS = Os.isFamily(Os.FAMILY_WINDOWS);
    static final boolean IS_MAC = Os.isFamily(Os.FAMILY_MAC);
    static final boolean IS_LINUX = Os.OS_NAME.startsWith("linux");
    static final boolean IS_SUNOS = Os.OS_NAME.startsWith("sunos");

    /**
     * Tries to find existing Java SDKs on this computer.
     * If no JDK found, returns possible directories to start file chooser.
     * @return suggested sdk home paths (sorted)
     */
    public static Set<String> suggestHomePaths() {
        return suggestHomePaths(false);
    }

    /**
     * Do the same as {@link #suggestHomePaths()} but always considers the embedded JRE,
     * for using in tests that are performed when the registry is not properly initialized
     * or that need the embedded JetBrains Runtime.
     */
    public static Set<String> suggestHomePaths(boolean forceEmbeddedJava) {
        JavaHomeFinderBasic javaFinder = getFinder().checkEmbeddedJava(forceEmbeddedJava);
        return javaFinder.findExistingJdks();
    }

    public static JavaHomeFinderBasic getFinder() {
        if (IS_WINDOWS) {
            return new JavaHomeFinderWindows(true);
        }
        if (IS_MAC) {
            return new JavaHomeFinderMac();
        }
        if (IS_LINUX) {
            return new JavaHomeFinderBasic().checkSpecifiedPaths(DEFAULT_JAVA_LINUX_PATHS);
        }
        if (IS_SUNOS) {
            return new JavaHomeFinderBasic().checkSpecifiedPaths("/usr/jdk");
        }
        return new JavaHomeFinderBasic();
    }

    public static final String[] DEFAULT_JAVA_LINUX_PATHS = {"/usr/java", "/opt/java", "/usr/lib/jvm"};
}
