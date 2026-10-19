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
package org.codehaus.plexus.classworlds;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Generates test fixture JARs at test time using the JDK's {@code javac} and {@code jar} tools,
 * eliminating the need to commit pre-compiled binary JARs to version control.
 *
 * <p>All JARs are written to {@code target/test-jars/} under the module base directory.
 * The generator is idempotent: if the directory already exists and is non-empty it is wiped
 * and rebuilt to ensure freshness.
 *
 * <p>Called once per test run from {@link AbstractClassWorldsTestCase#generateTestJars()}.
 */
public class TestJarGenerator {

    private final Path outputDir;
    private final JavaCompiler compiler;

    /** Map of compiled class name → bytecode, accumulated across compile() calls. */
    private final Map<String, byte[]> compiledClasses = new LinkedHashMap<>();

    public TestJarGenerator(Path outputDir) throws IOException {
        this.outputDir = outputDir;
        Files.createDirectories(outputDir);
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler available – tests must run on a JDK, not a JRE.");
        }
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /** Generate all test fixture JARs into {@link #outputDir}. */
    public void generate() throws Exception {
        generateSimpleJars();
        generateComponentJars();
        generateMiscJars();
    }

    /** Simple single-class and deadlock fixture JARs. */
    private void generateSimpleJars() throws Exception {
        // Simple stubs – no external dependencies
        compileClasses(
                null,
                src("a/A.java", "package a; public class A { public static void main(String[] a) {} }"),
                src("a/Aa.java", "package a; public class Aa { public static int main(String[] a) { return 15; } }"));
        jar(
                "a.jar",
                res("a.properties", "a properties\n"),
                res("common.properties", "common.properties"),
                cls("a/A.class"),
                cls("a/Aa.class"));
        compiledClasses.clear();

        compileClasses(
                null,
                src(
                        "b/B.java",
                        "package b;\npublic class B {\n"
                                + "  public static void main(String[] a, org.codehaus.plexus.classworlds.ClassWorld w) {}\n"
                                + "}"),
                src(
                        "b/Bb.java",
                        "package b;\npublic class Bb {\n"
                                + "  public static int main(String[] a, org.codehaus.plexus.classworlds.ClassWorld w) { return 45; }\n"
                                + "}"));
        jar(
                "b.jar",
                res("b.properties", "name = b.properties"),
                res("common.properties", "name = common.properties"),
                cls("b/B.class"),
                cls("b/Bb.class"));
        compiledClasses.clear();

        // b_old uses the legacy org.codehaus.classworlds.ClassWorld adapter
        compileClasses(
                null,
                src(
                        "b/B.java",
                        "package b;\npublic class B {\n"
                                + "  public static void main(String[] a, org.codehaus.classworlds.ClassWorld w) {}\n"
                                + "}"),
                src(
                        "b/Bb.java",
                        "package b;\npublic class Bb {\n"
                                + "  public static int main(String[] a, org.codehaus.classworlds.ClassWorld w) { return 0; }\n"
                                + "}"));
        jar("b_old.jar", cls("b/B.class"), cls("b/Bb.class"));
        compiledClasses.clear();

        compileClasses(null, src("c/C.java", "package c; public class C {}"));
        jar("c.jar", cls("c/C.class"));
        compiledClasses.clear();

        compileClasses(null, src("d/D.java", "package d; public class D {}"));
        jar("d.jar", cls("d/D.class"));
        compiledClasses.clear();

        // deadlock: cross-extends to trigger classloader deadlock test
        compileClasses(
                null,
                src("deadlock/B.java", "package deadlock; public class B {}"),
                src("deadlock/D.java", "package deadlock; public class D {}"),
                src("deadlock/A.java", "package deadlock; public class A extends B {}"),
                src("deadlock/C.java", "package deadlock; public class C extends D {}"));
        jar(
                "deadlock.jar",
                cls("deadlock/A.class"),
                cls("deadlock/B.class"),
                cls("deadlock/C.class"),
                cls("deadlock/D.class"));
        compiledClasses.clear();
    }

    /** Component0–5 plexus fixture JARs. */
    private void generateComponentJars() throws Exception {
        // circular: cross-reference between A and B, A has inner class C
        compileClasses(
                null,
                src(
                        "A.java",
                        "public class A {\n"
                                + "  public static B b = new B();\n"
                                + "  public static C c = new C();\n"
                                + "  public B foo(B arg) { return new B(); }\n"
                                + "  public C foo(C arg) { return new C(); }\n"
                                + "  public static A create(boolean arg) { return arg ? new A() : new C(); }\n"
                                + "  public static class C extends A {}\n"
                                + "}"),
                src(
                        "B.java",
                        "public class B {\n"
                                + "  public A a;\n"
                                + "  public A foo(A arg) { return new A(); }\n"
                                + "}"));
        jar(
                "circular-0.1.jar",
                res("pom.xml", circularPom()),
                res("src/main/java/org/A.java", circularSourceA()),
                res("src/main/java/org/B.java", circularSourceB()),
                res("META-INF/maven/test/circular/pom.xml", circularPom()),
                res(
                        "META-INF/maven/test/circular/pom.properties",
                        "#Generated by Maven\nversion=0.1\ngroupId=test\nartifactId=circular\n"),
                cls("A.class"),
                cls("A$C.class"),
                cls("B.class"));
        compiledClasses.clear();

        // component0..4: interface + impl pairs in org.codehaus.plexus package
        for (int i = 0; i <= 4; i++) {
            String iface = "Component" + i;
            String impl = "Default" + iface;
            compileClasses(
                    null,
                    src(
                            "org/codehaus/plexus/" + iface + ".java",
                            "package org.codehaus.plexus; public interface " + iface + " {}"),
                    src(
                            "org/codehaus/plexus/" + impl + ".java",
                            "package org.codehaus.plexus; public class " + impl + " implements " + iface + " {}"));
            jar(
                    "component" + i + "-1.0.jar",
                    res(
                            "META-INF/plexus/components.xml",
                            plexusComponents("org.codehaus.plexus." + iface, "org.codehaus.plexus." + impl)),
                    cls("org/codehaus/plexus/" + iface + ".class"),
                    cls("org/codehaus/plexus/" + impl + ".class"));
            compiledClasses.clear();
        }

        // component5: two versions with different method signatures (realm isolation test)
        compileClasses(
                null,
                src("test/Component5.java", "package test; public interface Component5 { String printOld(); }"),
                src(
                        "test/DefaultComponent5.java",
                        "package test; public class DefaultComponent5 implements Component5 {"
                                + " public String printOld() { return \"old\"; } }"));
        jar(
                "component5-1.0.jar",
                res("META-INF/plexus/components.xml", plexusComponents("test.Component5", "test.DefaultComponent5")),
                cls("test/Component5.class"),
                cls("test/DefaultComponent5.class"));
        compiledClasses.clear();

        compileClasses(
                null,
                src("test/Component5.java", "package test; public interface Component5 { String printNew(); }"),
                src(
                        "test/DefaultComponent5.java",
                        "package test; public class DefaultComponent5 implements Component5 {"
                                + " public String printNew() { return \"new\"; } }"));
        jar(
                "component5-2.0.jar",
                res("META-INF/plexus/components.xml", plexusComponents("test.Component5", "test.DefaultComponent5")),
                cls("test/Component5.class"),
                cls("test/DefaultComponent5.class"));
        compiledClasses.clear();
    }

    /** from-from, nested, and werkflow JARs. */
    private void generateMiscJars() throws Exception {
        // from-from: simple main class + META-INF/maven metadata
        compileClasses(
                null,
                src(
                        "com/from/from/from/Main.java",
                        "package com.from.from.from; public class Main { public static void main(String[] a) {} }"));
        jar(
                "from-from-0.0.1-from-load-import.jar",
                res("META-INF/maven/com.from/from-from/pom.xml", fromFromPom()),
                res(
                        "META-INF/maven/com.from/from-from/pom.properties",
                        "artifactId=from-from\ngroupId=com.from\nversion=0.0.1-from-load-import"),
                cls("com/from/from/from/Main.class"));
        compiledClasses.clear();

        // a.jar must exist before nested.jar (which embeds it)
        buildNestedJar();

        // werkflow: resource-only jar (no classes)
        buildWerkflowJar();
    }

    // -------------------------------------------------------------------------
    // Compilation helpers
    // -------------------------------------------------------------------------

    /**
     * Compile one or more in-memory Java source units.
     * Results are accumulated into {@link #compiledClasses}.
     *
     * @param extraClasspath additional JAR paths to add to the compile classpath (may be null)
     */
    private void compileClasses(List<Path> extraClasspath, SourceUnit... sources) throws Exception {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

            // Build classpath: current test classpath + any extras
            List<File> classpathFiles = currentClasspath();
            if (extraClasspath != null) {
                for (Path p : extraClasspath) {
                    classpathFiles.add(p.toFile());
                }
            }
            fm.setLocation(StandardLocation.CLASS_PATH, classpathFiles);

            // In-memory output file manager
            InMemoryFileManager memFm = new InMemoryFileManager(fm, compiledClasses);

            List<JavaFileObject> units = new ArrayList<>();
            for (SourceUnit su : sources) {
                units.add(su);
            }

            JavaCompiler.CompilationTask task = compiler.getTask(null, memFm, diagnostics, null, null, units);
            boolean ok = task.call();
            if (!ok) {
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (var d : diagnostics.getDiagnostics()) {
                    sb.append(d).append('\n');
                }
                throw new RuntimeException(sb.toString());
            }
        }
    }

    /** Collect the current JVM classpath entries as File objects. */
    private static List<File> currentClasspath() {
        List<File> result = new ArrayList<>();
        String cp = System.getProperty("java.class.path", "");
        for (String entry : cp.split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                result.add(new File(entry));
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // JAR assembly helpers
    // -------------------------------------------------------------------------

    /**
     * Assemble a JAR from a mix of {@link ClassEntry} (bytecode from compiledClasses)
     * and {@link ResourceEntry} (inline text content).
     */
    private void jar(String name, JarContent... entries) throws IOException {
        Path out = outputDir.resolve(name);
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out), mf)) {
            for (JarContent e : entries) {
                jos.putNextEntry(new JarEntry(e.entryName()));
                jos.write(e.bytes(compiledClasses));
                jos.closeEntry();
            }
        }
    }

    /** Build nested.jar: a.properties + lib/a.jar (a.jar embedded). */
    private void buildNestedJar() throws IOException {
        Path aJar = outputDir.resolve("a.jar");
        byte[] aJarBytes = Files.readAllBytes(aJar);

        Path out = outputDir.resolve("nested.jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out), mf)) {
            jos.putNextEntry(new JarEntry("nested.properties"));
            jos.write("nested.properties\n".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            jos.putNextEntry(new JarEntry("lib/a.jar"));
            jos.write(aJarBytes);
            jos.closeEntry();
        }
    }

    /** Build werkflow.jar: resource-only (XML files, no classes). */
    private void buildWerkflowJar() throws IOException {
        Path out = outputDir.resolve("resources/werkflow.jar");
        Files.createDirectories(out.getParent());

        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out), mf)) {
            addXmlEntry(jos, "org/codehaus/werkflow/personality/basic/basic-syntax.xml", "<syntax/>");
            addXmlEntry(jos, "org/codehaus/werkflow/personality/bpel4ws/bpel4ws-syntax.xml", "<syntax/>");
            addXmlEntry(jos, "org/codehaus/werkflow/personality/common/core-idioms.xml", "<idioms/>");
        }
    }

    private static void addXmlEntry(JarOutputStream jos, String name, String content) throws IOException {
        jos.putNextEntry(new JarEntry(name));
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }

    // -------------------------------------------------------------------------
    // Static resource strings
    // -------------------------------------------------------------------------

    private static String circularPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                + " http://maven.apache.org/maven-v4_0_0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>test</groupId>\n"
                + "  <artifactId>circular</artifactId>\n"
                + "  <version>0.1</version>\n"
                + "</project>\n";
    }

    private static String circularSourceA() {
        return "public class A {\n"
                + "    public static B b = new B();\n"
                + "    public static C c = new C();\n"
                + "    public B foo( B arg ) { return new B(); }\n"
                + "    public C foo( C arg ) { return new C(); }\n"
                + "    public static A create( boolean arg) { return arg ? new A() : new C(); }\n"
                + "    public static class C extends A {}\n"
                + "}\n";
    }

    private static String circularSourceB() {
        return "public class B {\n" + "    public A a;\n" + "    public A foo( A arg ) { return new A(); }\n" + "}\n";
    }

    private static String plexusComponents(String role, String impl) {
        return "<component-set>\n"
                + "  <components>\n"
                + "    <component>\n"
                + "      <role>" + role + "</role>\n"
                + "      <implementation>" + impl + "</implementation>\n"
                + "    </component>\n"
                + "  </components>\n"
                + "</component-set>\n";
    }

    private static String fromFromPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                + " http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>com.from</groupId>\n"
                + "  <artifactId>from-from</artifactId>\n"
                + "  <version>0.0.1-from-load-import</version>\n"
                + "</project>\n";
    }

    // -------------------------------------------------------------------------
    // DSL helpers for jar() entries
    // -------------------------------------------------------------------------

    interface JarContent {
        String entryName();

        byte[] bytes(Map<String, byte[]> compiled) throws IOException;
    }

    /** Bytecode entry — key is the slash-separated class name + ".class". */
    record ClassEntry(String name) implements JarContent {
        @Override
        public String entryName() {
            return name;
        }

        @Override
        public byte[] bytes(Map<String, byte[]> compiled) {
            // Strip ".class" to get the map key used by InMemoryFileManager
            String key = name.endsWith(".class") ? name.substring(0, name.length() - 6) : name;
            byte[] b = compiled.get(key);
            if (b == null) {
                throw new RuntimeException(
                        "No compiled bytecode for: " + key + " (available: " + compiled.keySet() + ")");
            }
            return b;
        }
    }

    /** Inline text resource entry. */
    record ResourceEntry(String name, String content) implements JarContent {
        @Override
        public String entryName() {
            return name;
        }

        @Override
        public byte[] bytes(Map<String, byte[]> compiled) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static ClassEntry cls(String name) {
        return new ClassEntry(name);
    }

    private static ResourceEntry res(String name, String content) {
        return new ResourceEntry(name, content);
    }

    // -------------------------------------------------------------------------
    // In-memory source unit
    // -------------------------------------------------------------------------

    static class SourceUnit extends SimpleJavaFileObject {
        private final String src;

        SourceUnit(String className, String src) {
            super(URI.create("mem:///" + className.replace('.', '/') + ".java"), Kind.SOURCE);
            this.src = src;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return src;
        }
    }

    private static SourceUnit src(String path, String src) {
        // path like "a/A.java" → class name "a.A"
        String className = path.replace('/', '.').replace(".java", "");
        return new SourceUnit(className, src);
    }

    // -------------------------------------------------------------------------
    // In-memory output file manager
    // -------------------------------------------------------------------------

    /**
     * Routes compiled {@code .class} output into a {@code Map<String, byte[]>}
     * instead of writing to disk.
     */
    static class InMemoryFileManager extends javax.tools.ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, byte[]> classBytes;

        InMemoryFileManager(StandardJavaFileManager delegate, Map<String, byte[]> classBytes) {
            super(delegate);
            this.classBytes = classBytes;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + ".class"), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            // key uses '/' separator (matches JarEntry names without ".class")
                            classBytes.put(className.replace('.', '/'), toByteArray());
                        }
                    };
                }
            };
        }
    }
}
