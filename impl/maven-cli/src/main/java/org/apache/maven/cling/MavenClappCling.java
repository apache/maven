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
package org.apache.maven.cling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.api.services.MavenException;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven CLAPP (Command Line App) entry point.
 * <p>
 * This class acts as the launcher for external Maven CLI tools ("CLAPPs") that ship their
 * own dependencies in {@code ${maven.home}/lib/clapp/<toolName>/} instead of requiring
 * those jars to be in the shared {@code ${maven.home}/lib/} directory.
 * <p>
 * The {@code maven.clapp.name} system property identifies which CLAPP to launch.
 * The CLAPP's jar directory is {@code ${maven.home}/lib/clapp/<toolName>/}.
 * That directory is scanned for {@code *.jar} files which are added to a child
 * {@link URLClassLoader} that delegates to the core Maven class-loader.
 * The CLAPP's main entry point class is then looked up via the
 * {@code maven.clapp.mainClass} system property and invoked.
 * <p>
 * This mechanism allows future {@code mvnXxx} tools to package tool-specific
 * dependencies in isolation without bloating the core Maven classpath.
 *
 * @since 4.1.0
 */
public class MavenClappCling extends ClingSupport {

    /**
     * System property that specifies the CLAPP tool name (e.g., {@code "mvnenc"}).
     * Used to locate {@code ${maven.home}/lib/clapp/<toolName>/}.
     */
    public static final String MAVEN_CLAPP_NAME_PROPERTY = "maven.clapp.name";

    /**
     * System property that specifies the fully-qualified main class name of the CLAPP tool.
     * That class must expose a {@code public static int main(String[], ClassWorld)} method.
     */
    public static final String MAVEN_CLAPP_MAIN_CLASS_PROPERTY = "maven.clapp.mainClass";

    /**
     * Exception thrown when a CLAPP tool cannot be loaded, configured, or launched.
     *
     * @since 4.1.0
     */
    public static class ClappException extends MavenException {
        public ClappException(String message) {
            super(message);
        }

        public ClappException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Relative path under {@code ${maven.home}} where per-CLAPP jar directories live.
     */
    static final String CLAPP_LIB_RELATIVE_PATH = "lib/clapp";

    /**
     * "Normal" Java entry point. Note: Maven uses ClassWorld Launcher and this entry point is NOT
     * used under normal circumstances.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenClappCling().run(args, null, null, null, false);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point: returning exitCode and accepts ClassWorld.
     * <p>
     * When {@code maven.clapp.name} and {@code maven.clapp.mainClass} system properties are set,
     * this method builds a per-CLAPP child classloader and delegates to the CLAPP's main class.
     * Otherwise, it falls back to the standard {@link MavenCling} behaviour.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        String clappName = System.getProperty(MAVEN_CLAPP_NAME_PROPERTY);
        String clappMainClass = System.getProperty(MAVEN_CLAPP_MAIN_CLASS_PROPERTY);

        if (clappName != null && !clappName.isBlank() && clappMainClass != null && !clappMainClass.isBlank()) {
            return launchClapp(clappName.trim(), clappMainClass.trim(), args, world);
        }

        // Fallback: behave as MavenCling when no CLAPP is configured
        return MavenCling.main(args, world);
    }

    /**
     * ClassWorld Launcher "embedded" entry point: returning exitCode and accepts ClassWorld and streams.
     */
    public static int main(
            String[] args,
            ClassWorld world,
            @Nullable InputStream stdIn,
            @Nullable OutputStream stdOut,
            @Nullable OutputStream stdErr)
            throws IOException {
        return new MavenClappCling(world).run(args, stdIn, stdOut, stdErr, true);
    }

    public MavenClappCling() {
        super();
    }

    public MavenClappCling(ClassWorld classWorld) {
        super(classWorld);
    }

    // -------------------------------------------------------------------------
    // ClingSupport contract – used when invoked as a fallback Maven build
    // -------------------------------------------------------------------------

    @Override
    protected Invoker createInvoker() {
        return new MavenInvoker(
                ProtoLookup.builder().addMapping(ClassWorld.class, classWorld).build(), null);
    }

    @Override
    protected Parser createParser() {
        return new MavenParser();
    }

    @Override
    protected ParserRequest.Builder createParserRequestBuilder(String[] args) {
        return ParserRequest.mvn(args, createMessageBuilderFactory());
    }

    // -------------------------------------------------------------------------
    // CLAPP launch logic
    // -------------------------------------------------------------------------

    /**
     * Constructs a per-CLAPP child class-loader, loads the CLAPP main class from it,
     * and invokes its {@code main(String[], ClassWorld)} method.
     *
     * @param clappName      the CLAPP tool name (e.g., {@code "mvnenc"})
     * @param clappMainClass fully-qualified name of the CLAPP entry-point class
     * @param args           command-line arguments
     * @param world          the ClassWorld shared with the Maven core
     * @return the exit code returned by the CLAPP
     * @throws IOException              if the CLAPP lib directory exists but cannot be read
     * @throws ClappException          if the CLAPP cannot be loaded or invocation fails
     * @throws IllegalArgumentException if the CLAPP tool name is invalid or attempts path traversal
     */
    static int launchClapp(String clappName, String clappMainClass, String[] args, ClassWorld world)
            throws IOException, ClappException {
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null || mavenHome.isBlank()) {
            throw new ClappException(
                    "System property 'maven.home' is not set; cannot locate CLAPP lib directory for: " + clappName);
        }

        // Validate tool name to prevent path traversal
        if (!clappName.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid CLAPP tool name: '" + clappName + "'");
        }

        Path baseDir = Paths.get(mavenHome).resolve(CLAPP_LIB_RELATIVE_PATH).normalize();
        Path clappLibDir = baseDir.resolve(clappName).normalize();
        if (!clappLibDir.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid CLAPP lib directory path traversal attempt for: " + clappName);
        }

        // Build the list of jar URLs from the CLAPP-specific lib directory
        List<URL> jarUrls = collectJarUrls(clappLibDir, clappName);

        // Create a child class-loader that sees the core classes + the CLAPP's own jars
        ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader clappLoader = new URLClassLoader(jarUrls.toArray(new URL[0]), parentLoader)) {
            Class<?> clazz = clappLoader.loadClass(clappMainClass);
            Method entryPoint = findEntryPointMethod(clazz);
            // Publish the CLAPP class-loader as the context class-loader so that
            // SPI / ServiceLoader mechanisms work correctly inside the CLAPP.
            Thread.currentThread().setContextClassLoader(clappLoader);
            return invokeEntryPoint(entryPoint, args, world);
        } catch (ClassNotFoundException e) {
            throw new ClappException("CLAPP '" + clappName + "': cannot find main class '" + clappMainClass + "'", e);
        } catch (NoSuchMethodException e) {
            throw new ClappException(
                    "CLAPP '" + clappName + "': main class '" + clappMainClass
                            + "' does not expose a supported entry point: public static int run(String[], ClassWorld) or main(...)",
                    e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new ClappException("CLAPP '" + clappName + "': invocation failed", cause != null ? cause : e);
        } catch (IllegalAccessException e) {
            throw new ClappException(
                    "CLAPP '" + clappName + "': cannot access entry point method of '" + clappMainClass + "'", e);
        } finally {
            // Restore class-loader so that the core Maven runtime is unaffected
            Thread.currentThread().setContextClassLoader(parentLoader);
        }
    }

    static Method findEntryPointMethod(Class<?> clazz) throws NoSuchMethodException {
        // 1. Preferred CLI tool entry point: run(String[], ClassWorld)
        try {
            return clazz.getMethod("run", String[].class, ClassWorld.class);
        } catch (NoSuchMethodException ignored) {
        }
        // 2. ClassWorld Cling convention: main(String[], ClassWorld)
        try {
            return clazz.getMethod("main", String[].class, ClassWorld.class);
        } catch (NoSuchMethodException ignored) {
        }
        // 3. Standard Java entry point: main(String[])
        return clazz.getMethod("main", String[].class);
    }

    static int invokeEntryPoint(Method method, String[] args, ClassWorld world)
            throws IllegalAccessException, InvocationTargetException {
        if (method.getParameterCount() == 2) {
            Object result = method.invoke(null, args, world);
            return result instanceof Integer ? (Integer) result : 0;
        } else {
            Object result = method.invoke(null, (Object) args);
            return result instanceof Integer ? (Integer) result : 0;
        }
    }

    /**
     * Scans {@code clappLibDir} for {@code *.jar} files and returns their {@link URL}s.
     * <p>
     * If the directory does not exist this method returns an empty list so that a CLAPP
     * that ships no extra jars (relying entirely on the core classpath) is still valid.
     *
     * @param clappLibDir directory to scan
     * @param clappName   tool name (only used for error messages)
     * @return list of jar {@link URL}s found in the directory (may be empty)
     * @throws IOException if the directory exists but cannot be listed
     */
    static List<URL> collectJarUrls(Path clappLibDir, String clappName) throws IOException {
        List<URL> urls = new ArrayList<>();
        if (!Files.exists(clappLibDir)) {
            // Intentionally not an error: CLAPPs that only use core jars need no extra directory
            return urls;
        }
        if (!Files.isDirectory(clappLibDir)) {
            throw new IOException(
                    "CLAPP lib path for '" + clappName + "' exists but is not a directory: " + clappLibDir);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(clappLibDir, "*.jar")) {
            for (Path jar : stream) {
                try {
                    urls.add(jar.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new IOException("Cannot convert CLAPP jar path to URL: " + jar, e);
                }
            }
        }
        return urls;
    }
}
