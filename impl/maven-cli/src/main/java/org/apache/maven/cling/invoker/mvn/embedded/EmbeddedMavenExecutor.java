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
package org.apache.maven.cling.invoker.mvn.embedded;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

import static java.util.Objects.requireNonNull;

/**
 * Embedded executor implementation, that invokes Maven from installation directory within this same JVM but in isolated
 * classloader. This class supports Maven 4.x and Maven 3.x as well.
 * The class world with Maven is kept in memory as long as instance of this class is not closed. Subsequent execution
 * requests over same installation home are cached.
 */
public class EmbeddedMavenExecutor implements Executor {
    protected static final class Context {
        private final Properties properties;
        private final URLClassLoader bootClassLoader;
        private final String version;
        private final Object classWorld;
        private final ClassLoader tccl;
        private final Function<ExecutorRequest, Integer> exec;

        public Context(
                Properties properties,
                URLClassLoader bootClassLoader,
                String version,
                Object classWorld,
                ClassLoader tccl,
                Function<ExecutorRequest, Integer> exec) {
            this.properties = properties;
            this.bootClassLoader = bootClassLoader;
            this.version = version;
            this.classWorld = classWorld;
            this.tccl = tccl;
            this.exec = exec;
        }
    }

    private final Properties originalProperties;
    private final ClassLoader originalClassLoader;
    private final ConcurrentHashMap<Path, Context> contexts;

    public EmbeddedMavenExecutor() {
        this.originalClassLoader = Thread.currentThread().getContextClassLoader();
        this.contexts = new ConcurrentHashMap<>();
        this.originalProperties = System.getProperties();
    }

    @Override
    public int execute(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);
        Context context = mayCreate(executorRequest);

        System.setProperties(context.properties);
        Thread.currentThread().setContextClassLoader(context.tccl);
        try {
            return context.exec.apply(executorRequest);
        } catch (Exception e) {
            throw new ExecutorException("Failed to execute", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            System.setProperties(originalProperties);
        }
    }

    protected Context mayCreate(ExecutorRequest executorRequest) {
        Path installation = executorRequest.installationDirectory();
        if (!Files.isDirectory(installation)) {
            throw new IllegalArgumentException("Installation directory must point to existing directory");
        }
        return contexts.computeIfAbsent(installation, k -> {
            Path mavenHome = installation.toAbsolutePath().normalize();
            Path boot = mavenHome.resolve("boot");
            Path m2conf = mavenHome.resolve("bin/m2.conf");
            if (!Files.isDirectory(boot) || !Files.isRegularFile(m2conf)) {
                throw new IllegalArgumentException("Installation directory does not point to Maven installation");
            }

            Properties properties = new Properties();
            properties.putAll(System.getProperties());
            properties.put(
                    "user.dir",
                    executorRequest.cwd().toAbsolutePath().normalize().toString());
            properties.put("maven.multiModuleProjectDirectory", executorRequest.cwd().toAbsolutePath().normalize().toString());
            properties.put(
                    "user.home",
                    executorRequest
                            .userHomeDirectory()
                            .toAbsolutePath()
                            .normalize()
                            .toString());
            properties.put("maven.home", mavenHome.toString());
            properties.put("maven.mainClass", "org.apache.maven.cling.MavenCling");
            properties.put(
                    "library.jline.path", mavenHome.resolve("lib/jline-native").toString());

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
                Class<?> cliClass =
                        (Class<?>) launcherClass.getMethod("getMainClass").invoke(launcher);
                String version = getMavenVersion(cliClass.getClassLoader());
                Function<ExecutorRequest, Integer> exec;

                if (version.startsWith("3.")) {
                    // 3.x
                    Constructor<?> newMavenCli = cliClass.getConstructor(classWorld.getClass());
                    Object mavenCli = newMavenCli.newInstance(classWorld);
                    Class<?>[] parameterTypes = {String[].class, String.class, PrintStream.class, PrintStream.class};
                    Method doMain = cliClass.getMethod("doMain", parameterTypes);
                    exec = r -> {
                        try {
                            return (int) doMain.invoke(mavenCli, new Object[] {
                                r.parserRequest().args().toArray(new String[0]),
                                r.cwd().toString(),
                                null,
                                null
                            });
                        } catch (Exception e) {
                            throw new ExecutorException("Failed to execute", e);
                        }
                    };
                } else {
                    // assume 4.x
                    Method mainMethod = cliClass.getMethod("main", String[].class, classWorld.getClass());
                    exec = r -> {
                        try {
                            return (int) mainMethod.invoke(
                                    null, r.parserRequest().args().toArray(new String[0]), classWorld);
                        } catch (Exception e) {
                            throw new ExecutorException("Failed to execute", e);
                        }
                    };
                }

                return new Context(properties, bootClassLoader, version, classWorld, cliClass.getClassLoader(), exec);
            } catch (Exception e) {
                throw new ExecutorException("Failed to create executor", e);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                System.setProperties(originalProperties);
            }
        });
    }

    @Override
    public void close() throws ExecutorException {
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

    protected void doClose(Context context) throws Exception {
        Thread.currentThread().setContextClassLoader(context.bootClassLoader);
        try {
            try {
                ((Closeable) context.classWorld).close();
            } finally {
                context.bootClassLoader.close();
            }
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

    public String getMavenVersion(ClassLoader classLoader) throws IOException {
        Properties props = new Properties();
        try (InputStream is =
                classLoader.getResourceAsStream("/META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
            if (is != null) {
                props.load(is);
            }
            String version = props.getProperty("version");
            if (version != null) {
                return version;
            }
            return "unknown";
        }
    }
}
