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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class JavaHomeFinderMac extends JavaHomeFinderBasic {
    public static final String JAVA_HOME_FIND_UTIL = "/usr/libexec/java_home";

    static String defaultJavaLocation = "/Library/Java/JavaVirtualMachines";

    public JavaHomeFinderMac() {
        super();

        registerFinder(() -> {
            Set<String> result = new TreeSet<>();
            Iterable<Path> roots = FileSystems.getDefault().getRootDirectories();
            roots.forEach(root -> {
                result.addAll(scanAll(root.resolve(defaultJavaLocation), true));
            });
            roots.forEach(root -> {
                result.addAll(scanAll(root.resolve("System/Library/Java/JavaVirtualMachines"), true));
            });
            return result;
        });

        registerFinder(() -> {
            Path jdk = getPathInUserHome("Library/Java/JavaVirtualMachines");
            return jdk != null ? scanAll(jdk, true) : Collections.emptySet();
        });
        registerFinder(() -> scanAll(getSystemDefaultJavaHome(), false));
    }

    protected Path getSystemDefaultJavaHome() {
        String homePath = null;
        if (new File(JAVA_HOME_FIND_UTIL).canExecute()) {
            try {
                homePath = execCommand(JAVA_HOME_FIND_UTIL);
            } catch (Exception e) {
                // TODO: log ?
            }
        }
        if (homePath != null) {
            return Paths.get(homePath);
        }
        return null;
    }

    @Override
    protected List<Path> listPossibleJdkHomesFromInstallRoot(Path path) {
        return Arrays.asList(path, path.resolve("/Home"), path.resolve("Contents/Home"));
    }

    @Override
    protected List<Path> listPossibleJdkInstallRootsFromHomes(Path file) {
        List<Path> result = new ArrayList<>();
        result.add(file);

        Path home = file.getFileName();
        if (home != null && home.toString().equalsIgnoreCase("Home")) {
            Path parentFile = file.getParent();
            if (parentFile != null) {
                result.add(parentFile);

                Path contents = parentFile.getFileName();
                if (contents != null && contents.toString().equalsIgnoreCase("Contents")) {
                    Path parentParentFile = parentFile.getParent();
                    if (parentParentFile != null) {
                        result.add(parentParentFile);
                    }
                }
            }
        }

        return result;
    }
}
