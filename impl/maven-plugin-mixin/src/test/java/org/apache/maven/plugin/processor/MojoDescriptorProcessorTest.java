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
package org.apache.maven.plugin.processor;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link MojoDescriptorProcessor} using the JDK in-process compiler.
 *
 * <p>Each test compiles a small in-memory source file that uses the Maven plugin
 * annotations ({@code @Mojo}, {@code @Parameter}, etc.) from maven-api-core on
 * the classpath, with the processor JAR on the processor path.  Generated
 * resources (plugin.xml) and source files (factory classes) are intercepted by
 * a {@link CapturingFileManager} and asserted in-memory — no disk I/O.</p>
 */
class MojoDescriptorProcessorTest {

    /** Resolved classpath jars needed for compiling test sources. */
    private static List<File> classpathJars;

    /** The processor JAR (contains the compiled MojoDescriptorProcessor). */
    private static File processorJar;

    @BeforeAll
    static void resolveClasspath() {
        // Processor JAR — built from this module
        processorJar = resolveFile(
                "impl/maven-plugin-mixin/target/maven-plugin-mixin-4.1.0-SNAPSHOT.jar",
                "maven-plugin-mixin/4.1.0-SNAPSHOT/maven-plugin-mixin-4.1.0-SNAPSHOT.jar");

        // Annotation JARs needed on the compilation classpath
        File apiCore = resolveFile(
                "api/maven-api-core/target/maven-api-core-4.1.0-SNAPSHOT.jar",
                "maven-api-core/4.1.0-SNAPSHOT/maven-api-core-4.1.0-SNAPSHOT.jar");
        File apiAnnotations =
                resolveFile(null, "maven-api-annotations/4.1.0-SNAPSHOT/maven-api-annotations-4.1.0-SNAPSHOT.jar");

        classpathJars = List.of(apiCore, apiAnnotations);
    }

    /**
     * Try to resolve a JAR first from the worktree build output, then from the local Maven repo.
     */
    private static File resolveFile(String worktreeRelative, String m2Relative) {
        String worktreeBase =
                System.getProperty("processor.worktree", "/opt/data/oss/maven/.worktrees/plugin-processor");
        String m2Base = System.getProperty(
                "processor.m2", System.getProperty("user.home") + "/.m2/repository/org/apache/maven");

        if (worktreeRelative != null) {
            File f = new File(worktreeBase, worktreeRelative);
            if (f.isFile()) {
                return f;
            }
        }
        if (m2Relative != null) {
            File f = new File(m2Base, m2Relative);
            if (f.isFile()) {
                return f;
            }
        }
        fail("Cannot resolve JAR: worktree=" + worktreeRelative + " m2=" + m2Relative);
        return null; // unreachable
    }

    // -------------------------------------------------------------------------
    // Test 1: Minimal @Mojo → plugin.xml + factory
    // -------------------------------------------------------------------------

    @Test
    void minimalMojoGeneratesPluginXmlAndFactory() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                @Mojo(name = "greet")
                public class GreetMojo {
                    public void execute() {}
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.GreetMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "com.example",
                        "maven.plugin.artifactId", "example-maven-plugin",
                        "maven.plugin.version", "1.0.0"));

        assertTrue(result.success, () -> "Compilation failed:\n" + result.diagnosticsToString());

        // plugin.xml assertions
        String pluginXml = result.getResource("META-INF/maven/plugin.xml");
        assertNotNull(pluginXml, "plugin.xml must be generated");
        assertContains(pluginXml, "<goal>greet</goal>");
        assertContains(pluginXml, "<implementation>com.example.GreetMojo</implementation>");
        assertContains(pluginXml, "<groupId>com.example</groupId>");
        assertContains(pluginXml, "<artifactId>example-maven-plugin</artifactId>");
        assertContains(pluginXml, "<version>1.0.0</version>");
        // goalPrefix derived from "example-maven-plugin": strips "-maven-plugin" suffix → "example"
        assertContains(pluginXml, "<goalPrefix>example</goalPrefix>");
        assertContains(pluginXml, "<language>java</language>");
        // default projectRequired = true
        assertContains(pluginXml, "<projectRequired>true</projectRequired>");

        // Factory class assertion
        String factorySource = result.getGeneratedSource("com.example.GreetMojoMojoFactory");
        assertNotNull(factorySource, "Factory source must be generated");
        assertContains(
                factorySource, "@org.apache.maven.api.di.Named(\"com.example:example-maven-plugin:1.0.0:greet\")");
        assertContains(factorySource, "public class GreetMojoMojoFactory extends GreetMojo");
    }

    // -------------------------------------------------------------------------
    // Test 2: @Mojo with @Parameter fields (readonly → editable inversion)
    // -------------------------------------------------------------------------

    @Test
    void mojoWithParametersGeneratesParametersBlock() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                import org.apache.maven.api.plugin.annotations.Parameter;
                @Mojo(name = "compile", defaultPhase = "compile")
                public class CompileMojo {
                    @Parameter(property = "source", defaultValue = "17", required = true)
                    private String sourceVersion;

                    @Parameter(readonly = true, property = "project.basedir")
                    private java.io.File basedir;

                    @Parameter(name = "customName", alias = "alt")
                    private int threshold;

                    public void execute() {}
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.CompileMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "org.apache.maven.plugins",
                        "maven.plugin.artifactId", "maven-compiler-plugin",
                        "maven.plugin.version", "4.0.0"));

        assertTrue(result.success, () -> "Compilation failed:\n" + result.diagnosticsToString());

        String pluginXml = result.getResource("META-INF/maven/plugin.xml");
        assertNotNull(pluginXml, "plugin.xml must be generated");

        // defaultPhase
        assertContains(pluginXml, "<phase>compile</phase>");

        // goalPrefix derived from "maven-compiler-plugin" → "compiler"
        assertContains(pluginXml, "<goalPrefix>compiler</goalPrefix>");

        // <parameters> block
        assertContains(pluginXml, "<parameters>");

        // sourceVersion parameter
        assertContains(pluginXml, "<name>sourceVersion</name>");
        assertContains(pluginXml, "<type>java.lang.String</type>");
        assertContains(pluginXml, "<required>true</required>");
        assertContains(pluginXml, "<expression>${source}</expression>");
        assertContains(pluginXml, "<defaultValue>17</defaultValue>");

        // basedir: readonly=true → editable=false
        assertContains(pluginXml, "<name>basedir</name>");
        // The readonly param should generate editable=false
        assertTrue(
                pluginXml.contains("<editable>false</editable>"),
                "readonly=true should produce editable=false in plugin.xml");
        assertContains(pluginXml, "<expression>${project.basedir}</expression>");

        // customName / alias
        assertContains(pluginXml, "<name>customName</name>");
        assertContains(pluginXml, "<alias>alt</alias>");

        // sourceVersion: readonly defaults to false → editable=true
        // We need to check that the sourceVersion parameter has editable=true
        // Since there are multiple editable elements, verify non-readonly defaults to editable=true
        assertTrue(
                pluginXml.contains("<editable>true</editable>"),
                "readonly=false (default) should produce editable=true");
    }

    // -------------------------------------------------------------------------
    // Test 3: @Mojo with @After → afterLinks block
    // -------------------------------------------------------------------------

    @Test
    void mojoWithAfterGeneratesAfterLinksBlock() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                import org.apache.maven.api.plugin.annotations.After;
                @Mojo(name = "post-compile")
                @After(phase = "compile")
                public class PostCompileMojo {
                    public void execute() {}
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.PostCompileMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "com.example",
                        "maven.plugin.artifactId", "example-plugin",
                        "maven.plugin.version", "1.0.0"));

        assertTrue(result.success, () -> "Compilation failed:\n" + result.diagnosticsToString());

        String pluginXml = result.getResource("META-INF/maven/plugin.xml");
        assertNotNull(pluginXml, "plugin.xml must be generated");
        assertContains(pluginXml, "<afterLinks>");
        assertContains(pluginXml, "<afterLink>");
        assertContains(pluginXml, "<phase>compile</phase>");
        // default type is PROJECT
        assertContains(pluginXml, "<type>PROJECT</type>");
    }

    // -------------------------------------------------------------------------
    // Test 4: @Mojo with @Execute → executePhase/executeGoal
    // -------------------------------------------------------------------------

    @Test
    void mojoWithExecuteGeneratesExecuteFields() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                import org.apache.maven.api.plugin.annotations.Execute;
                @Mojo(name = "report")
                @Execute(phase = "test-compile", lifecycle = "custom")
                public class ReportMojo {
                    public void execute() {}
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.ReportMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "com.example",
                        "maven.plugin.artifactId", "example-plugin",
                        "maven.plugin.version", "2.0.0"));

        assertTrue(result.success, () -> "Compilation failed:\n" + result.diagnosticsToString());

        String pluginXml = result.getResource("META-INF/maven/plugin.xml");
        assertNotNull(pluginXml, "plugin.xml must be generated");
        assertContains(pluginXml, "<executePhase>test-compile</executePhase>");
        assertContains(pluginXml, "<executeLifecycle>custom</executeLifecycle>");
    }

    // -------------------------------------------------------------------------
    // Test 5: Missing required -A options → ERROR diagnostic
    // -------------------------------------------------------------------------

    @Test
    void missingRequiredOptionsEmitsErrorDiagnostic() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                @Mojo(name = "noop")
                public class NoopMojo {
                    public void execute() {}
                }
                """;

        // Omit all required -A options
        CompilationResult result = compile(List.of(inMemorySource("com.example.NoopMojo", source)), Map.of());

        // The processor should emit ERROR diagnostics for each missing option
        assertFalse(result.success, "Compilation should fail when required -A options are missing");

        String errors = result.diagnosticsToString();
        assertContains(errors, "maven.plugin.groupId");
        assertContains(errors, "maven.plugin.artifactId");
        assertContains(errors, "maven.plugin.version");
    }

    // -------------------------------------------------------------------------
    // Test 6: @Mojo on abstract class → ERROR diagnostic
    // -------------------------------------------------------------------------

    @Test
    void mojoOnAbstractClassEmitsErrorDiagnostic() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                @Mojo(name = "bad")
                public abstract class AbstractBadMojo {
                    public abstract void execute();
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.AbstractBadMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "com.example",
                        "maven.plugin.artifactId", "example-plugin",
                        "maven.plugin.version", "1.0.0"));

        assertFalse(result.success, "Compilation should fail for @Mojo on abstract class");
        String errors = result.diagnosticsToString();
        assertContains(errors, "abstract");
    }

    // -------------------------------------------------------------------------
    // Test 7: deriveGoalPrefix() — direct static method test
    // -------------------------------------------------------------------------

    @Test
    void deriveGoalPrefixMavenXxxPlugin() {
        assertEquals("compiler", MojoDescriptorProcessor.deriveGoalPrefix("maven-compiler-plugin"));
    }

    @Test
    void deriveGoalPrefixXxxMavenPlugin() {
        // "my-maven-plugin": longer suffix "-maven-plugin" matched first → "my"
        assertEquals("my", MojoDescriptorProcessor.deriveGoalPrefix("my-maven-plugin"));
    }

    @Test
    void deriveGoalPrefixSimpleArtifactId() {
        assertEquals("simple", MojoDescriptorProcessor.deriveGoalPrefix("simple"));
    }

    @Test
    void deriveGoalPrefixPluginSuffix() {
        assertEquals("spotbugs", MojoDescriptorProcessor.deriveGoalPrefix("spotbugs-plugin"));
    }

    @Test
    void deriveGoalPrefixNullOrEmpty() {
        assertEquals("", MojoDescriptorProcessor.deriveGoalPrefix(null));
        assertEquals("", MojoDescriptorProcessor.deriveGoalPrefix(""));
    }

    @Test
    void deriveGoalPrefixMavenPluginOnly() {
        // "maven-plugin" → strip "maven-" → "plugin" → strip "-plugin" suffix → ""
        // Actually: "maven-plugin" → strip "maven-" → "plugin" → strip "-plugin" suffix → not applicable
        // (ends with "-plugin" but the remaining after stripping "maven-" is "plugin",
        //  which doesn't end with "-plugin" but IS "plugin" — no further strip)
        // Wait, let's check: s = "plugin", s.endsWith("-plugin") → false
        assertEquals("plugin", MojoDescriptorProcessor.deriveGoalPrefix("maven-plugin"));
    }

    // -------------------------------------------------------------------------
    // Test: explicit goalPrefix option overrides derivation
    // -------------------------------------------------------------------------

    @Test
    void explicitGoalPrefixOverridesDerived() {
        String source = """
                package com.example;
                import org.apache.maven.api.plugin.annotations.Mojo;
                @Mojo(name = "run")
                public class RunMojo {
                    public void execute() {}
                }
                """;

        CompilationResult result = compile(
                List.of(inMemorySource("com.example.RunMojo", source)),
                Map.of(
                        "maven.plugin.groupId", "com.example",
                        "maven.plugin.artifactId", "maven-compiler-plugin",
                        "maven.plugin.version", "1.0.0",
                        "maven.plugin.goalPrefix", "custom-prefix"));

        assertTrue(result.success, () -> "Compilation failed:\n" + result.diagnosticsToString());

        String pluginXml = result.getResource("META-INF/maven/plugin.xml");
        assertNotNull(pluginXml, "plugin.xml must be generated");
        assertContains(pluginXml, "<goalPrefix>custom-prefix</goalPrefix>");
        // Should NOT contain the derived prefix
        assertFalse(pluginXml.contains("<goalPrefix>compiler</goalPrefix>"));
    }

    // =========================================================================
    //  Compilation harness
    // =========================================================================

    /**
     * Compiles the given in-memory sources with the MojoDescriptorProcessor on
     * the processor path and the maven-api-core annotations on the classpath.
     */
    private CompilationResult compile(List<JavaFileObject> sources, Map<String, String> processorOptions) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system Java compiler available (run tests with a JDK, not a JRE)");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager stdFm = compiler.getStandardFileManager(diagnostics, null, null);

        try {
            // Build classpath: maven-api-core + maven-api-annotations for the test sources
            stdFm.setLocation(StandardLocation.CLASS_PATH, classpathJars);

            // Processor path: the processor JAR
            stdFm.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, List.of(processorJar));

        } catch (IOException e) {
            fail("Failed to configure file manager locations: " + e.getMessage());
        }

        // Wrap in a CapturingFileManager to intercept generated outputs
        CapturingFileManager capturingFm = new CapturingFileManager(stdFm);

        // Build compiler options
        List<String> options = new ArrayList<>();
        // Processor options as -Akey=value
        for (Map.Entry<String, String> opt : processorOptions.entrySet()) {
            options.add("-A" + opt.getKey() + "=" + opt.getValue());
        }
        // Explicitly name the processor to use
        options.add("-processor");
        options.add("org.apache.maven.plugin.processor.MojoDescriptorProcessor");
        // Ensure annotation processing is not suppressed
        // (--proc-full requires JDK 23+; -proc:full is JDK 21-compatible for enabling processing)

        StringWriter compilerOutput = new StringWriter();

        JavaCompiler.CompilationTask task =
                compiler.getTask(compilerOutput, capturingFm, diagnostics, options, null, sources);
        boolean success = task.call();

        return new CompilationResult(success, diagnostics, capturingFm);
    }

    // =========================================================================
    //  In-memory source file
    // =========================================================================

    private static JavaFileObject inMemorySource(String className, String code) {
        return new SimpleJavaFileObject(
                URI.create("mem:///" + className.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    // =========================================================================
    //  CapturingFileManager — intercepts generated sources and resources
    // =========================================================================

    /**
     * A {@link ForwardingJavaFileManager} that captures source files generated
     * via {@code Filer.createSourceFile()} and resources via {@code Filer.createResource()}.
     */
    private static class CapturingFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        /** Generated source files keyed by class name. */
        final Map<String, String> generatedSources = new HashMap<>();
        /** Generated resource files keyed by relative path (e.g. "META-INF/maven/plugin.xml"). */
        final Map<String, String> generatedResources = new HashMap<>();

        CapturingFileManager(StandardJavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling)
                throws IOException {
            if (kind == JavaFileObject.Kind.SOURCE) {
                // This is a generated source file
                return new InMemoryJavaFileObject(className, kind, content -> generatedSources.put(className, content));
            }
            if (kind == JavaFileObject.Kind.CLASS) {
                // We don't need the compiled class bytes, but we must provide a valid output
                return new InMemoryJavaFileObject(className, kind, content -> {});
            }
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }

        @Override
        public FileObject getFileForOutput(
                JavaFileManager.Location location, String packageName, String relativeName, FileObject sibling)
                throws IOException {
            String key = packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + "/" + relativeName;
            return new InMemoryFileObject(key, content -> generatedResources.put(key, content));
        }

        @Override
        public FileObject getFileForInput(JavaFileManager.Location location, String packageName, String relativeName)
                throws IOException {
            throw new java.io.FileNotFoundException("No input resource: " + relativeName);
        }
    }

    /** Callback interface for capturing written content. */
    @FunctionalInterface
    private interface ContentSink {
        void accept(String content);
    }

    /**
     * In-memory JavaFileObject that captures content written to its output stream.
     */
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final ContentSink sink;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InMemoryJavaFileObject(String className, Kind kind, ContentSink sink) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
            this.sink = sink;
        }

        @Override
        public OutputStream openOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    baos.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    baos.write(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    super.close();
                    sink.accept(baos.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
            };
        }

        @Override
        public java.io.Writer openWriter() {
            return new StringWriter() {
                @Override
                public void close() throws IOException {
                    super.close();
                    sink.accept(this.toString());
                }
            };
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * In-memory FileObject for resource files (e.g. plugin.xml).
     */
    private static class InMemoryFileObject extends SimpleJavaFileObject {
        private final ContentSink sink;

        InMemoryFileObject(String path, ContentSink sink) {
            super(URI.create("mem:///" + path), Kind.OTHER);
            this.sink = sink;
        }

        @Override
        public OutputStream openOutputStream() {
            return new OutputStream() {
                private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                @Override
                public void write(int b) {
                    baos.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    baos.write(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    super.close();
                    sink.accept(baos.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
            };
        }

        @Override
        public java.io.Writer openWriter() {
            return new StringWriter() {
                @Override
                public void close() throws IOException {
                    super.close();
                    sink.accept(this.toString());
                }
            };
        }
    }

    // =========================================================================
    //  CompilationResult
    // =========================================================================

    private static class CompilationResult {
        final boolean success;
        final DiagnosticCollector<JavaFileObject> diagnostics;
        final CapturingFileManager fileManager;

        CompilationResult(
                boolean success, DiagnosticCollector<JavaFileObject> diagnostics, CapturingFileManager fileManager) {
            this.success = success;
            this.diagnostics = diagnostics;
            this.fileManager = fileManager;
        }

        /** Returns generated resource content by relative path, or null if not found. */
        String getResource(String path) {
            return fileManager.generatedResources.get(path);
        }

        /** Returns generated source content by fully-qualified class name, or null. */
        String getGeneratedSource(String className) {
            return fileManager.generatedSources.get(className);
        }

        String diagnosticsToString() {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                sb.append(d.getKind()).append(": ").append(d.getMessage(null)).append("\n");
            }
            return sb.toString();
        }
    }

    // =========================================================================
    //  Assertion helpers
    // =========================================================================

    private static void assertContains(String text, String substring) {
        assertTrue(text.contains(substring), () -> "Expected to find '" + substring + "' in:\n" + truncate(text, 2000));
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "\n... [truncated at " + maxLen + " of " + s.length() + " chars]";
    }
}
