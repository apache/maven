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
package org.apache.maven.plugin.descriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;
import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginDescriptorTest {

    @Test
    void lifecycleMappingReadsRewrittenJar(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("plugin.jar");
        writeLifecycle(jar, "first");
        assertNotNull(lifecycleMapping(jar, "first"));

        // Rewritten in place, as a plugin rebuilt in the reactor is.
        writeLifecycle(jar, "second-and-longer-than-the-first");
        assertNotNull(lifecycleMapping(jar, "second-and-longer-than-the-first"));

        // Nothing keeps the jar open, so it can be deleted (fails on Windows while it is open).
        Files.delete(jar);
    }

    private static Lifecycle lifecycleMapping(Path jar, String id) throws Exception {
        // Only the file of the plugin artifact is read
        Artifact artifact = (Artifact) Proxy.newProxyInstance(
                Artifact.class.getClassLoader(),
                new Class<?>[] {Artifact.class},
                (proxy, method, args) -> "getFile".equals(method.getName()) ? jar.toFile() : null);
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setPluginArtifact(artifact);
        return descriptor.getLifecycleMapping(id);
    }

    private static void writeLifecycle(Path jar, String id) throws IOException {
        try (OutputStream out = Files.newOutputStream(jar);
                JarOutputStream jarOut = new JarOutputStream(out)) {
            jarOut.putNextEntry(new ZipEntry("META-INF/maven/lifecycle.xml"));
            jarOut.write(("<lifecycles><lifecycle><id>" + id + "</id></lifecycle></lifecycles>")
                    .getBytes(StandardCharsets.UTF_8));
            jarOut.closeEntry();
        }
    }
}
