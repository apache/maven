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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 * Implementation of ToolchainDiscoverer service
 */
@Named
@Singleton
public class DefaultToolchainDiscoverer implements ToolchainDiscoverer {

    @Override
    public PersistedToolchains discoverToolchains() {
        try {
            Set<String> jdks = JavaHomeFinder.suggestHomePaths(true);
            List<ToolchainModel> tcs = new ArrayList<>();
            for (Path jdk : jdks.stream().map(Paths::get).collect(Collectors.toList())) {
                ToolchainModel tc = getToolchainModel(jdk);
                if (tc != null) {
                    tcs.add(tc);
                }
            }
            tcs.sort(getToolchainModelComparator());
            return new PersistedToolchains(org.apache.maven.api.toolchain.PersistedToolchains.newBuilder()
                    .toolchains(tcs)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ToolchainModel getToolchainModel(Path jdk) throws IOException, InterruptedException {
        Path bin = jdk.resolve("bin");
        Path java = bin.resolve("java");
        if (!Files.isRegularFile(java)) {
            java = bin.resolve("java.exe");
            if (!Files.isRegularFile(java)) {
                System.err.println("Unable to find java executable for " + jdk);
                return null;
            }
        }
        Path temp = Files.createTempFile("jdk-opts-", ".out");
        new ProcessBuilder()
                .command(java.toString(), "-XshowSettings:properties", "-version")
                .redirectError(temp.toFile())
                .start()
                .waitFor();
        List<String> lines = Files.readAllLines(temp);
        Files.delete(temp);
        String jdkHome = lines.stream()
                .filter(l -> l.contains("java.home"))
                .map(l -> l.replaceFirst(".*=\\s*(.*)", "$1"))
                .findFirst()
                .get();

        Map<String, String> properties = new LinkedHashMap<>();
        for (String name : Arrays.asList(
                "java.version", "java.runtime.name", "java.runtime.version", "java.vendor", "java.vendor.version")) {
            String v = lines.stream()
                    .filter(l -> l.contains(name))
                    .map(l -> l.replaceFirst(".*=\\s*(.*)", "$1"))
                    .findFirst()
                    .orElse(null);
            String k = name.substring(5);
            if (v != null) {
                properties.put(k, v);
            }
        }
        return ToolchainModel.newBuilder()
                .type("jdk")
                .provides(properties)
                .configuration(new XmlNodeImpl(
                        "configuration",
                        null,
                        null,
                        Collections.singletonList(new XmlNodeImpl("jdkHome", jdkHome)),
                        null))
                .build();
    }

    Comparator<ToolchainModel> getToolchainModelComparator() {
        return Comparator.comparing((ToolchainModel tc) -> tc.getProvides().get("vendor"))
                .thenComparing(tc -> tc.getProvides().get("version"), this::compareVersion);
    }

    int compareVersion(String v1, String v2) {
        String[] s1 = v1.split("\\.");
        String[] s2 = v2.split("\\.");
        return compare(s1, s2);
    }

    static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // A null element is less than a non-null element
                if (oa == null || ob == null) {
                    return oa == null ? -1 : 1;
                }
                int v = oa.compareTo(ob);
                if (v != 0) {
                    return v;
                }
            }
        }
        return a.length - b.length;
    }
}
