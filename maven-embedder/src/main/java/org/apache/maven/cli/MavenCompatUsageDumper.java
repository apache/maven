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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Helper class that will dump all maven-compat invocations to a file
 */
public class MavenCompatUsageDumper {

    public static final String DUMPER_ENABLED = MavenCompatUsageDumper.class.getName() + ".enabled";
    public static final String DUMPER_FILE = MavenCompatUsageDumper.class.getName() + ".file";
    // number of lines from the stack trace to discard
    // those come from the interception mechanism
    private static final int LINES_TO_IGNORE = 6;

    private final boolean enabled;
    private final PrintWriter writer;
    private final boolean printStackTrace;
    private final Map<String, Set<List<String>>> invocations = new ConcurrentHashMap<>();

    public MavenCompatUsageDumper(Properties sysProps, Properties userProps) {
        Properties props = new Properties();
        props.putAll(sysProps);
        props.putAll(userProps);
        this.enabled = Boolean.parseBoolean(props.getProperty(DUMPER_ENABLED, "false"));
        PrintWriter pw = null;
        boolean printStackTrace = false;
        if (this.enabled) {
            String file = props.getProperty(DUMPER_FILE);
            if (file != null) {
                try {
                    pw = new PrintWriter(Files.newBufferedWriter(Paths.get(file)));
                    printStackTrace = true;
                } catch (IOException e) {
                    System.err.println("Unable to write to " + file + ", using stdout (" + e + ")");
                    pw = new PrintWriter(new OutputStreamWriter(System.out));
                }
            }
            if (pw == null) {
                pw = new PrintWriter(new OutputStreamWriter(System.out));
            }
        }
        this.writer = pw;
        this.printStackTrace = printStackTrace;
    }

    private void log(MethodInvocation methodInvocation) {
        String method = methodInvocation.getMethod().toGenericString();
        Set<List<String>> stackTraces = this.invocations.computeIfAbsent(method, k -> ConcurrentHashMap.newKeySet());
        StringWriter sw = new StringWriter();
        new Throwable().printStackTrace(new PrintWriter(sw));
        List<String> stackTrace =
                Stream.of(sw.toString().split("\n")).skip(LINES_TO_IGNORE).collect(Collectors.toList());
        if (stackTraces.add(stackTrace)) {
            if (!isInternalCall(stackTrace) && !isFromDefaultModelBuilder(stackTrace)) {
                synchronized (writer) {
                    if (printStackTrace) {
                        writer.println(method);
                        stackTrace.forEach(writer::println);
                    } else {
                        writer.println("Using maven-compat deprecated method: " + method);
                    }
                    if (writer.checkError()) {
                        System.err.println("MavenCompatDumper in error");
                    }
                }
            }
        }
    }

    /**
     * Disable calls from the DefaultProjectBuildingHelper
     */
    private boolean isFromDefaultModelBuilder(List<String> stackTrace) {
        return stackTrace.get(0).contains("org.apache.maven.project.DefaultProjectBuildingHelper")
                && stackTrace.get(1).contains("org.apache.maven.project.DefaultModelBuildingListener")
                && stackTrace.get(2).contains("org.apache.maven.model.building.DefaultModelBuilder.fireEvent");
    }

    /**
     * Disable calls from within maven-compat
     */
    private boolean isInternalCall(List<String> stackTrace) {
        return stackTrace.stream().anyMatch(s -> s.contains(MavenCompatUsageDumper.class.getName()));
    }

    private Object invoke(MethodInvocation methodInvocation) throws Throwable {
        log(methodInvocation);
        return methodInvocation.proceed();
    }

    private boolean matchClass(Class<?> aClass) {
        if (!aClass.isInterface() && !aClass.isEnum()) {
            return isMavenCompat(aClass) || ArtifactRepository.class.isAssignableFrom(aClass);
        }
        return false;
    }

    private boolean matchMethod(Method method) {
        return isMavenCompat(method.getDeclaringClass());
    }

    private boolean isMavenCompat(Class<?> aClass) {
        ClassLoader cl = aClass != null ? aClass.getClassLoader() : null;
        URL url = cl != null ? cl.getResource(aClass.getName().replace('.', '/') + ".class") : null;
        return url != null && url.getPath().contains("/maven-compat-");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Matcher<Class<?>> getClassMatcher() {
        return new AbstractMatcher<Class<?>>() {
            @Override
            public boolean matches(Class<?> aClass) {
                return matchClass(aClass);
            }
        };
    }

    public Matcher<Method> getMethodMatcher() {
        return new AbstractMatcher<Method>() {
            @Override
            public boolean matches(Method method) {
                return matchMethod(method);
            }
        };
    }

    public MethodInterceptor getMethodInterceptor() {
        return this::invoke;
    }
}
