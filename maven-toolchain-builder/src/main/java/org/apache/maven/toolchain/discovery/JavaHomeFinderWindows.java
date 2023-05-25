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

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Named("windows")
@Singleton
class JavaHomeFinderWindows extends JavaHomeFinderBasic {

    private static final String REG_COMMAND = "reg query HKLM\\SOFTWARE\\JavaSoft\\JDK /s /v JavaHome";

    private static final Pattern JAVA_HOME_PATTERN =
            Pattern.compile("^\\s+JavaHome\\s+REG_SZ\\s+(\\S.+\\S)\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static Set<String> gatherHomePaths(CharSequence text) {
        Set<String> paths = new TreeSet<>();
        Matcher m = JAVA_HOME_PATTERN.matcher(text);
        while (m.find()) {
            paths.add(m.group(1));
        }
        return paths;
    }

    JavaHomeFinderWindows(boolean registeredJdks) {
        if (registeredJdks) {
            // Whether the OS is 64-bit (**important**: it's not the same as [com.intellij.util.system.CpuArch]).
            String pfx86 = getEnvironmentVariable("ProgramFiles(x86)");
            boolean os64bit = pfx86 != null && !pfx86.trim().isEmpty();
            if (os64bit) {
                registerFinder(() -> readRegisteredLocations(" /reg:64"));
                registerFinder(() -> readRegisteredLocations(" /reg:32"));
            } else {
                registerFinder(() -> readRegisteredLocations(""));
            }
        }
        registerFinder(this::guessPossibleLocations);
    }

    private Set<String> readRegisteredLocations(String bitness) {
        String cmd = REG_COMMAND + bitness;
        try {
            CharSequence registryLines = execCommand(cmd);
            Set<String> registeredPaths = gatherHomePaths(registryLines);
            Set<Path> folders = new TreeSet<>();
            for (String rp : registeredPaths) {
                Path r = Paths.get(rp);
                Path parent = r.getParent();
                if (parent != null && Files.exists(parent)) {
                    folders.add(parent);
                } else if (Files.exists(r)) {
                    folders.add(r);
                }
            }
            return scanAll(folders, true);
        } catch (InterruptedException ie) {
            return Collections.emptySet();
        } catch (Exception e) {
            log.warn("Unable to detect registered JDK using the following command: $cmd", e);
            return Collections.emptySet();
        }
    }

    private Set<String> guessPossibleLocations() {
        Iterable<Path> fsRoots = FileSystems.getDefault().getRootDirectories();
        Set<Path> roots = new HashSet<>();
        for (Path root : fsRoots) {
            if (Files.exists(root)) {
                roots.add(root.resolve("Program Files/Java"));
                roots.add(root.resolve("Program Files (x86)/Java"));
                roots.add(root.resolve("Java"));
            }
        }
        Path puh = getPathInUserHome(".jdks");
        if (puh != null) {
            roots.add(puh);
        }
        return scanAll(roots, true);
    }
}
