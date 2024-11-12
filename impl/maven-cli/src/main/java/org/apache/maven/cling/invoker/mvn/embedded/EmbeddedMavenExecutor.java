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
import java.util.stream.Stream;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

import static java.util.Objects.requireNonNull;

/**
 * Embedded invoker implementation, that invokes Maven from installation directory within this same JVM but in isolated
 * classloader.
 */
public class EmbeddedMavenExecutor implements Executor {
    protected static final class Context {
        private final Properties properties;
        private final URLClassLoader bootClassLoader;
        private final Object classWorld;
        private final Method mainMethod;

        public Context(Properties properties, URLClassLoader bootClassLoader, Object classWorld, Method mainMethod) {
            this.properties = properties;
            this.bootClassLoader = bootClassLoader;
            this.classWorld = classWorld;
            this.mainMethod = mainMethod;
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

        Path installation = executorRequest.installationDirectory();
        if (!Files.isDirectory(installation)) {
            throw new IllegalArgumentException("Installation directory must point to existing directory");
        }
        Context context = mayCreate(installation);

        System.setProperties(context.properties);
        Thread.currentThread().setContextClassLoader(context.bootClassLoader);
        try {
            return (int) context.mainMethod.invoke(
                    null, executorRequest.parserRequest().args().toArray(new String[0]), context.classWorld);
        } catch (Exception e) {
            throw new ExecutorException("Failed to execute", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            System.setProperties(originalProperties);
        }
    }

    protected Context mayCreate(Path installation) {
        return contexts.computeIfAbsent(installation, k -> {
            Path mavenHome = installation.toAbsolutePath().normalize();
            Path boot = mavenHome.resolve("boot");
            Path m2conf = mavenHome.resolve("bin/m2.conf");
            if (!Files.isDirectory(boot) || !Files.isRegularFile(m2conf)) {
                throw new IllegalArgumentException("Installation directory does not point to Maven installation");
            }

            Properties properties = System.getProperties();
            properties.put("maven.home", mavenHome.toString());
            properties.put("maven.mainClass", "org.apache.maven.cling.MavenCling");
            properties.put(
                    "library.jline.path", mavenHome.resolve("lib/jline-native").toString());

            System.setProperties(properties);
            URLClassLoader bootClassLoader = createMavenBootClassLoader(boot, Collections.emptyList());
            Thread.currentThread().setContextClassLoader(bootClassLoader);
            try {
                Class<?> launcherClass = bootClassLoader.loadClass("org.codehaus.plexus.classworlds.launcher.Launcher");
                Class<?> classWorldClass = bootClassLoader.loadClass("org.codehaus.plexus.classworlds.ClassWorld");
                Object launcher = launcherClass.getDeclaredConstructor().newInstance();
                Method configure = launcherClass.getMethod("configure", InputStream.class);
                try (InputStream inputStream = Files.newInputStream(m2conf)) {
                    configure.invoke(launcher, inputStream);
                }
                Object classWorld = launcherClass.getMethod("getWorld").invoke(launcher);
                Class<?> cliClass =
                        (Class<?>) launcherClass.getMethod("getMainClass").invoke(launcher);
                Method mainMethod = cliClass.getMethod("main", String[].class, classWorldClass);

                return new Context(properties, bootClassLoader, classWorld, mainMethod);
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
                context.bootClassLoader.close();
            } finally {
                ((Closeable) context.classWorld).close();
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
}
