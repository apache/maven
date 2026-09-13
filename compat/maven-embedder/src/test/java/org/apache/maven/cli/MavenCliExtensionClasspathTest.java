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
package org.apache.maven.cli;

import javax.tools.ToolProvider;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
class MavenCliExtensionClasspathTest {
    @TempDir
    Path directory;

    @Test
    void exportsClassPathDescriptors() throws Exception {
        Path jar = extensionJar("archive", true);
        Path withoutDescriptor = extensionJar("unexported", false);
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path descriptor = classes.resolve("META-INF/maven/extension.xml");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, descriptor("directory"));
        Path resource = classes.resolve("extension/directory/marker.txt");
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, "directory");

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try (ClassWorld world = new ClassWorld("plexus.core", getClass().getClassLoader())) {
            MavenCli cli = new MavenCli();
            CliRequest request = new CliRequest(
                    new String[] {
                        "-Dmaven.ext.class.path=" + withoutDescriptor + File.pathSeparator + jar + File.pathSeparator
                                + classes
                    },
                    world);
            request.workingDirectory = directory.toString();
            cli.cli(request);
            cli.properties(request);
            cli.logging(request);
            PlexusContainer container = cli.container(request);
            try {
                CoreExports exports = container.lookup(CoreExports.class);
                ClassRealmManager manager = container.lookup(ClassRealmManager.class);
                Plugin plugin = new Plugin();
                plugin.setGroupId("extension");
                plugin.setArtifactId("test-plugin");
                plugin.setVersion("1.0");
                ClassRealm realm = manager.createPluginRealm(
                        plugin,
                        null,
                        null,
                        Map.of("", manager.getMavenApiRealm()),
                        List.of(
                                new DefaultArtifact("extension:archive:jar:2.0").setFile(jar.toFile()),
                                new DefaultArtifact("extension:directory:jar:2.0").setFile(classes.toFile()),
                                new DefaultArtifact("extension:unexported:jar:2.0")
                                        .setFile(withoutDescriptor.toFile())));
                assertAll(
                        () -> assertTrue(exports.getExportedArtifacts()
                                .containsAll(List.of(
                                        "extension:archive", "extension:directory", "org.apache.maven:maven-core"))),
                        () -> assertArrayEquals(
                                new java.net.URL[] {withoutDescriptor.toUri().toURL()}, realm.getURLs()),
                        () -> assertNotNull(manager.getMavenApiRealm().getResource("extension/archive/marker.txt")),
                        () -> assertNotNull(manager.getMavenApiRealm().getResource("extension/directory/marker.txt")),
                        () -> assertNull(manager.getMavenApiRealm().getResource("extension/unexported/marker.txt")),
                        () -> assertNotNull(realm.getResource("extension/unexported/marker.txt")),
                        () -> assertSame(
                                container.getContainerRealm(),
                                realm.loadClass("extension.archive.Marker").getClassLoader()),
                        () -> assertSame(
                                realm,
                                realm.loadClass("extension.unexported.Marker").getClassLoader()),
                        () -> assertNull(
                                world.getClassRealm("plexus.core").getResource("extension/archive/marker.txt")),
                        () -> assertTrue(exports.getExportedPackages().containsKey("org.apache.maven.api")));
            } finally {
                container.dispose();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private Path extensionJar(String name, boolean descriptor) throws Exception {
        Path classes = Files.createDirectory(directory.resolve(name));
        Path source = classes.resolve("Marker.java");
        Files.writeString(source, "package extension." + name + "; public class Marker {}");
        assertEquals(
                0,
                ToolProvider.getSystemJavaCompiler()
                        .run(null, null, null, "-d", classes.toString(), source.toString()));
        Path jar = directory.resolve(name + "-1.0.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            String classFile = "extension/" + name + "/Marker.class";
            output.putNextEntry(new JarEntry(classFile));
            Files.copy(classes.resolve(classFile), output);
            output.closeEntry();
            if (descriptor) {
                output.putNextEntry(new JarEntry("META-INF/maven/extension.xml"));
                output.write(descriptor(name).getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
            output.putNextEntry(new JarEntry("extension/" + name + "/marker.txt"));
            output.write(name.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return jar;
    }

    private String descriptor(String name) {
        return """
                <extension>
                  <exportedPackages><exportedPackage>extension.%s</exportedPackage></exportedPackages>
                  <exportedArtifacts><exportedArtifact>extension:%s</exportedArtifact></exportedArtifacts>
                </extension>
                """.formatted(name, name);
    }
}
