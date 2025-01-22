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
package org.apache.maven.cling.executor.embedded;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

import static java.util.Objects.requireNonNull;

/**
 * Embedded executor implementation, that invokes Maven from installation directory within this same JVM but in isolated
 * classloader. This class supports Maven 4.x and Maven 3.x as well. The ClassWorld of Maven is kept in memory as
 * long as instance of this class is not closed. Subsequent execution requests over same installation home are cached.
 */
public class EmbeddedMavenExecutor implements Executor {
    protected static final Map<String, String> MAIN_CLASSES = Map.of(
            "mvn",
            "org.apache.maven.cling.MavenCling",
            "mvnenc",
            "org.apache.maven.cling.MavenEncCling",
            "mvnsh",
            "org.apache.maven.cling.MavenShellCling");

    protected static final class Context {
        private final URLClassLoader bootClassLoader;
        private final String version;
        private final Object classWorld;
        private final Set<String> originalClassRealmIds;
        private final ClassLoader tccl;
        private final Function<ExecutorRequest, Integer> exec;

        private Context(
                URLClassLoader bootClassLoader,
                String version,
                Object classWorld,
                Set<String> originalClassRealmIds,
                ClassLoader tccl,
                Function<ExecutorRequest, Integer> exec) {
            this.bootClassLoader = bootClassLoader;
            this.version = version;
            this.classWorld = classWorld;
            this.originalClassRealmIds = originalClassRealmIds;
            this.tccl = tccl;
            this.exec = exec;
        }
    }

    protected static class Key {
        private final Path installationDirectory;
        private final String command;

        private Key(Path installationDirectory, String command) {
            this.installationDirectory = installationDirectory;
            this.command = command;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(installationDirectory, key.installationDirectory)
                    && Objects.equals(command, key.command);
        }

        @Override
        public int hashCode() {
            return Objects.hash(installationDirectory, command);
        }
    }

    protected final boolean cacheContexts;
    protected final boolean useMavenArgsEnv;
    protected final AtomicBoolean closed;
    protected final PrintStream originalStdout;
    protected final PrintStream originalStderr;
    protected final Properties originalProperties;
    protected final ClassLoader originalClassLoader;
    protected final ConcurrentHashMap<Key, Context> contexts;

    public EmbeddedMavenExecutor() {
        this(true, true);
    }

    public EmbeddedMavenExecutor(boolean cacheContexts, boolean useMavenArgsEnv) {
        this.cacheContexts = cacheContexts;
        this.useMavenArgsEnv = useMavenArgsEnv;
        this.closed = new AtomicBoolean(false);
        this.originalStdout = System.out;
        this.originalStderr = System.err;
        this.originalClassLoader = Thread.currentThread().getContextClassLoader();
        this.contexts = new ConcurrentHashMap<>();
        this.originalProperties = new Properties();
        this.originalProperties.putAll(System.getProperties());
    }

    @Override
    public int execute(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        if (closed.get()) {
            throw new ExecutorException("Executor is closed");
        }
        validate(executorRequest);
        Context context = mayCreate(executorRequest);

        Thread.currentThread().setContextClassLoader(context.tccl);
        try {
            return context.exec.apply(executorRequest);
        } catch (Exception e) {
            throw new ExecutorException("Failed to execute", e);
        } finally {
            try {
                disposeRuntimeCreatedRealms(context);
            } finally {
                System.setOut(originalStdout);
                System.setErr(originalStderr);
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                System.setProperties(originalProperties);
                if (!cacheContexts) {
                    doClose(context);
                }
            }
        }
    }

    protected void disposeRuntimeCreatedRealms(Context context) {
        try {
            Method getRealms = context.classWorld.getClass().getMethod("getRealms");
            Method disposeRealm = context.classWorld.getClass().getMethod("disposeRealm", String.class);
            List<Object> realms = (List<Object>) getRealms.invoke(context.classWorld);
            for (Object realm : realms) {
                String realmId = (String) realm.getClass().getMethod("getId").invoke(realm);
                if (!context.originalClassRealmIds.contains(realmId)) {
                    disposeRealm.invoke(context.classWorld, realmId);
                }
            }
        } catch (Exception e) {
            throw new ExecutorException("Failed to dispose runtime created realms", e);
        }
    }

    @Override
    public String mavenVersion(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);
        if (closed.get()) {
            throw new ExecutorException("Executor is closed");
        }
        return mayCreate(executorRequest).version;
    }

    protected Context mayCreate(ExecutorRequest executorRequest) {
        Path mavenHome = ExecutorRequest.getCanonicalPath(executorRequest.installationDirectory());
        String command = executorRequest.command();
        Key key = new Key(mavenHome, command);
        if (cacheContexts) {
            return contexts.computeIfAbsent(key, k -> doCreate(mavenHome, executorRequest));
        } else {
            return doCreate(mavenHome, executorRequest);
        }
    }

    protected Context doCreate(Path mavenHome, ExecutorRequest executorRequest) {
        if (!Files.isDirectory(mavenHome)) {
            throw new IllegalArgumentException("Installation directory must point to existing directory: " + mavenHome);
        }
        if (!MAIN_CLASSES.containsKey(executorRequest.command())) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " does not support command " + executorRequest.command());
        }
        if (executorRequest.environmentVariables().isPresent()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " does not support environment variables");
        }
        if (executorRequest.jvmArguments().isPresent()) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " does not support jvmArguments");
        }
        Path boot = mavenHome.resolve("boot");
        Path m2conf = mavenHome.resolve("bin/m2.conf");
        if (!Files.isDirectory(boot) || !Files.isRegularFile(m2conf)) {
            throw new IllegalArgumentException(
                    "Installation directory does not point to Maven installation: " + mavenHome);
        }

        ArrayList<String> mavenArgs = new ArrayList<>();
        String mavenArgsEnv = System.getenv("MAVEN_ARGS");
        if (useMavenArgsEnv && mavenArgsEnv != null && !mavenArgsEnv.isEmpty()) {
            Arrays.stream(mavenArgsEnv.split(" "))
                    .filter(s -> !s.trim().isEmpty())
                    .forEach(s -> mavenArgs.add(0, s));
        }

        Properties properties = prepareProperties(executorRequest);

        System.setProperties(properties);
        URLClassLoader bootClassLoader = createMavenBootClassLoader(boot, Collections.emptyList());
        Thread.currentThread().setContextClassLoader(bootClassLoader);
        try {
            Class<?> launcherClass = bootClassLoader.loadClass("org.codehaus.plexus.classworlds.launcher.Launcher");
            Object launcher = launcherClass.getDeclaredConstructor().newInstance();
            Method configure = launcherClass.getMethod("configure", InputStream.class);
            try (InputStream inputStream = Files.newInputStream(m2conf)) {
                configure.invoke(launcher, inputStream);
            }
            Object classWorld = launcherClass.getMethod("getWorld").invoke(launcher);
            Set<String> originalClassRealmIds = new HashSet<>();

            // collect pre-created (in m2.conf) class realms as "original ones"; the rest are created at runtime
            Method getRealms = classWorld.getClass().getMethod("getRealms");
            List<Object> realms = (List<Object>) getRealms.invoke(classWorld);
            for (Object realm : realms) {
                Method realmGetId = realm.getClass().getMethod("getId");
                originalClassRealmIds.add((String) realmGetId.invoke(realm));
            }

            Class<?> cliClass =
                    (Class<?>) launcherClass.getMethod("getMainClass").invoke(launcher);
            String version = getMavenVersion(cliClass);
            Function<ExecutorRequest, Integer> exec;

            if (version.startsWith("3.")) {
                // 3.x
                if (!ExecutorRequest.MVN.equals(executorRequest.command())) {
                    throw new IllegalArgumentException(getClass().getSimpleName() + "w/ mvn3 does not support command "
                            + executorRequest.command());
                }
                Constructor<?> newMavenCli = cliClass.getConstructor(classWorld.getClass());
                Object mavenCli = newMavenCli.newInstance(classWorld);
                Class<?>[] parameterTypes = {String[].class, String.class, PrintStream.class, PrintStream.class};
                Method doMain = cliClass.getMethod("doMain", parameterTypes);
                exec = r -> {
                    System.setProperties(prepareProperties(r));
                    InputStream originalInputStream = System.in;
                    r.stdinProvider().ifPresent(System::setIn);
                    try {
                        ArrayList<String> args = new ArrayList<>(mavenArgs);
                        args.addAll(r.arguments());
                        PrintStream stdout = r.stdoutConsumer().isEmpty()
                                ? null
                                : new PrintStream(r.stdoutConsumer().orElseThrow(), true);
                        PrintStream stderr = r.stderrConsumer().isEmpty()
                                ? null
                                : new PrintStream(r.stderrConsumer().orElseThrow(), true);
                        return (int) doMain.invoke(mavenCli, new Object[] {
                            args.toArray(new String[0]), r.cwd().toString(), stdout, stderr
                        });
                    } catch (Exception e) {
                        throw new ExecutorException("Failed to execute", e);
                    } finally {
                        System.setIn(originalInputStream);
                    }
                };
            } else {
                // assume 4.x
                Method mainMethod = cliClass.getMethod(
                        "main",
                        String[].class,
                        classWorld.getClass(),
                        InputStream.class,
                        OutputStream.class,
                        OutputStream.class);
                exec = r -> {
                    System.setProperties(prepareProperties(r));
                    try {
                        ArrayList<String> args = new ArrayList<>(mavenArgs);
                        args.addAll(r.arguments());
                        return (int) mainMethod.invoke(
                                null,
                                args.toArray(new String[0]),
                                classWorld,
                                r.stdinProvider().orElse(System.in),
                                r.stdoutConsumer().orElse(System.out),
                                r.stderrConsumer().orElse(System.err));
                    } catch (Exception e) {
                        throw new ExecutorException("Failed to execute", e);
                    }
                };
            }

            return new Context(
                    bootClassLoader, version, classWorld, originalClassRealmIds, cliClass.getClassLoader(), exec);
        } catch (Exception e) {
            throw new ExecutorException("Failed to create executor", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            System.setProperties(originalProperties);
        }
    }

    protected Properties prepareProperties(ExecutorRequest request) {
        System.setProperties(null); // this "inits" them!

        Properties properties = new Properties();
        properties.putAll(System.getProperties()); // get mandatory/expected init-ed above

        properties.setProperty("user.dir", request.cwd().toString());
        properties.setProperty("user.home", request.userHomeDirectory().toString());

        Path mavenHome = request.installationDirectory();
        properties.setProperty("maven.home", mavenHome.toString());
        properties.setProperty(
                "maven.multiModuleProjectDirectory", request.cwd().toString());
        String mainClass = requireNonNull(MAIN_CLASSES.get(request.command()), "mainClass");
        properties.setProperty("maven.mainClass", mainClass);

        if (request.jvmSystemProperties().isPresent()) {
            properties.putAll(request.jvmSystemProperties().get());
        }

        return properties;
    }

    @Override
    public void close() throws ExecutorException {
        if (closed.compareAndExchange(false, true)) {
            try {
                ArrayList<Exception> exceptions = new ArrayList<>();
                for (Context context : contexts.values()) {
                    try {
                        doClose(context);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
                if (!exceptions.isEmpty()) {
                    ExecutorException e = new ExecutorException("Could not close cleanly");
                    exceptions.forEach(e::addSuppressed);
                    throw e;
                }
            } finally {
                System.setProperties(originalProperties);
            }
        }
    }

    protected void doClose(Context context) throws ExecutorException {
        Thread.currentThread().setContextClassLoader(context.bootClassLoader);
        try {
            try {
                ((Closeable) context.classWorld).close();
            } finally {
                context.bootClassLoader.close();
            }
        } catch (Exception e) {
            throw new ExecutorException("Failed to close cleanly", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    protected void validate(ExecutorRequest executorRequest) throws ExecutorException {}

    protected URLClassLoader createMavenBootClassLoader(Path boot, List<URL> extraClasspath) {
        ArrayList<URL> urls = new ArrayList<>(extraClasspath);
        try (Stream<Path> stream = Files.list(boot)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(f -> {
                        try {
                            urls.add(f.toUri().toURL());
                        } catch (MalformedURLException e) {
                            throw new ExecutorException("Failed to build classpath: " + f, e);
                        }
                    });
        } catch (IOException e) {
            throw new ExecutorException("Failed to build classpath: " + e, e);
        }
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Invalid Maven home directory; boot is empty");
        }
        return new URLClassLoader(
                urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
    }

    protected String getMavenVersion(Class<?> clazz) throws IOException {
        Properties props = new Properties();
        try (InputStream is = clazz.getResourceAsStream("/META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
            if (is != null) {
                props.load(is);
            }
            String version = props.getProperty("version");
            if (version != null) {
                return version;
            }
            return UNKNOWN_VERSION;
        }
    }
}
