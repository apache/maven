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
package org.apache.maven.cling.invoker;

import javax.tools.ToolProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.classrealm.ClassRealmManager;
import org.apache.maven.cling.invoker.mvn.MavenContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlexusContainerCapsuleFactoryTest {
    @TempDir
    Path directory;

    private ClassWorld world;
    private ClassLoader originalClassLoader;
    private DefaultPlexusContainer container;

    @BeforeEach
    void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        world = new ClassWorld("plexus.core", getClass().getClassLoader());
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (container != null) {
                container.dispose();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            world.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exportsPackagesToPlugins(boolean jar) throws Exception {
        Path extension = extension(jar, true);
        createContainer(List.of("-Dmaven.ext.class.path=" + extension));

        ClassRealm pluginRealm = pluginRealm(List.of());
        Class<?> exported = pluginRealm.loadClass("extension.api.Exported");
        assertSame(container.getContainerRealm(), exported.getClassLoader());
        assertThrows(ClassNotFoundException.class, () -> pluginRealm.loadClass("extension.internal.Private"));

        ClassRealm coreRealm = world.getClassRealm("plexus.core");
        assertEquals(0, coreRealm.getURLs().length);
        assertThrows(ClassNotFoundException.class, () -> coreRealm.loadClass("extension.api.Exported"));
        ClassRealm otherExtension = coreRealm.createChildRealm("coreExtension>other");
        assertThrows(ClassNotFoundException.class, () -> otherExtension.loadClass("extension.api.Exported"));
        assertTrue(container.lookup(CoreExports.class).getExportedPackages().containsKey("org.apache.maven.api"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void filtersExportedArtifactsFromPlugins(boolean jar) throws Exception {
        Path extension = extension(jar, true);
        createContainer(List.of("-Dmaven.ext.class.path=" + extension));

        Path dependency = Files.createDirectory(directory.resolve("dependency"));
        Path retained = Files.createDirectory(directory.resolve("retained"));
        ClassRealm pluginRealm = pluginRealm(List.of(
                new DefaultArtifact("extension:api:jar:2.0").setFile(dependency.toFile()),
                new DefaultArtifact("extension:retained:jar:1.0").setFile(retained.toFile())));
        assertArrayEquals(new java.net.URL[] {retained.toUri().toURL()}, pluginRealm.getURLs());
        assertTrue(container.lookup(CoreExports.class).getExportedArtifacts().contains("org.apache.maven:maven-core"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void descriptorIsOptional(boolean jar) throws Exception {
        Path extension = extension(jar, false);
        createContainer(List.of("-Dmaven.ext.class.path=" + extension));

        assertSame(
                container.getContainerRealm(),
                container
                        .getContainerRealm()
                        .loadClass("extension.api.Exported")
                        .getClassLoader());
        ClassRealm pluginRealm =
                pluginRealm(List.of(new DefaultArtifact("extension:api:jar:2.0").setFile(extension.toFile())));
        assertArrayEquals(new java.net.URL[] {extension.toUri().toURL()}, pluginRealm.getURLs());
        assertFalse(container.lookup(CoreExports.class).getExportedPackages().containsKey("extension.api"));
    }

    @Test
    void keepsCoreRealmWithoutExtensions() throws Exception {
        createContainer(List.of());
        assertSame(world.getClassRealm("plexus.core"), container.getContainerRealm());
    }

    @Test
    void mergesClassPathDescriptors() throws Exception {
        Path withoutDescriptor = extension(true, false);
        Path jar = extension(true, true);
        Path classes = extension(false, true);
        Files.writeString(classes.resolve("META-INF/maven/extension.xml"), """
                <extension>
                  <exportedPackages><exportedPackage>extension.internal</exportedPackage></exportedPackages>
                  <exportedArtifacts><exportedArtifact>extension:internal</exportedArtifact></exportedArtifacts>
                </extension>
                """);
        createContainer(List.of("-Dmaven.ext.class.path=" + withoutDescriptor + File.pathSeparator + jar
                + File.pathSeparator + classes));

        CoreExports exports = container.lookup(CoreExports.class);
        assertTrue(exports.getExportedArtifacts().containsAll(List.of("extension:api", "extension:internal")));
        ClassRealm pluginRealm = pluginRealm(List.of());
        assertSame(
                container.getContainerRealm(),
                pluginRealm.loadClass("extension.api.Exported").getClassLoader());
        assertSame(
                container.getContainerRealm(),
                pluginRealm.loadClass("extension.internal.Private").getClassLoader());
    }

    private void createContainer(List<String> args) throws Exception {
        var request = new MavenParser()
                .parseInvocation(ParserRequest.mvn(args, new JLineMessageBuilderFactory())
                        .cwd(directory)
                        .userHome(directory)
                        .embedded(true)
                        .build());
        var context =
                new MavenContext(request, true, (MavenOptions) request.options().orElseThrow());
        context.loggerFactory = LoggerFactory.getILoggerFactory();
        context.loggerLevel = Slf4jConfiguration.Level.INFO;
        var invoker = new MavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, world).build(), null);
        container = new PlexusContainerCapsuleFactory<MavenContext>().container(invoker, context, (i, c) -> List.of());
    }

    private ClassRealm pluginRealm(List<Artifact> artifacts) throws Exception {
        Plugin plugin = new Plugin();
        plugin.setGroupId("extension");
        plugin.setArtifactId("test-plugin");
        plugin.setVersion("1.0");
        ClassRealmManager manager = container.lookup(ClassRealmManager.class);
        return manager.createPluginRealm(plugin, null, null, Map.of("", manager.getMavenApiRealm()), artifacts);
    }

    private Path extension(boolean jar, boolean descriptor) throws Exception {
        Path root = Files.createTempDirectory(directory, "extension");
        Path classes = Files.createDirectory(root.resolve("classes"));
        for (String name : List.of("extension.api.Exported", "extension.internal.Private")) {
            int separator = name.lastIndexOf('.');
            Path source = root.resolve(name.substring(separator + 1) + ".java");
            Files.writeString(
                    source,
                    "package " + name.substring(0, separator) + "; public class " + name.substring(separator + 1)
                            + " {}");
            assertEquals(
                    0,
                    ToolProvider.getSystemJavaCompiler()
                            .run(null, null, null, "-d", classes.toString(), source.toString()));
        }
        if (descriptor) {
            Path xml = classes.resolve("META-INF/maven/extension.xml");
            Files.createDirectories(xml.getParent());
            Files.writeString(xml, """
                    <extension>
                      <exportedPackages>
                        <exportedPackage>extension.api</exportedPackage>
                      </exportedPackages>
                      <exportedArtifacts>
                        <exportedArtifact>extension:api</exportedArtifact>
                      </exportedArtifacts>
                    </extension>
                    """);
        }
        if (!jar) {
            return classes;
        }
        Path archive = root.resolve("extension-1.0.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(archive));
                var files = Files.walk(classes)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(
                        new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        return archive;
    }
}
