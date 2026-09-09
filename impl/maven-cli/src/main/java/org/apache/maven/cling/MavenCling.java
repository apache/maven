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

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Maven CLI "new-gen".
 * <p>
 * This is the default entry point for Maven. When launched via ClassWorlds, it checks the
 * {@code maven.mainClass} system property and delegates to the specified class if set.
 * This allows external tools (Maven Wrapper, IDEs) to launch Maven without setting
 * the main class property — {@code MavenCling} is used as the default.
 */
public class MavenCling extends ClingSupport {
    /**
     * System property used by launcher scripts to select the CLI implementation.
     */
    static final String MAVEN_MAIN_CLASS_PROPERTY = "maven.mainClass";

    /**
     * "Normal" Java entry point. Note: Maven uses ClassWorld Launcher and this entry point is NOT used under normal
     * circumstances.
     */
    public static void main(String[] args) throws IOException {
        int exitCode = new MavenCling().run(args, null, null, null, false);
        System.exit(exitCode);
    }

    /**
     * ClassWorld Launcher "enhanced" entry point: returning exitCode and accepts Class World.
     * <p>
     * When the {@code maven.mainClass} system property is set to a class other than {@code MavenCling},
     * this method delegates to that class's {@code main(String[], ClassWorld)} method via reflection.
     * This makes {@code MavenCling} the default entry point that external tools (such as the Maven Wrapper
     * or IDEs) can rely on without needing to set the {@code maven.mainClass} property.
     */
    public static int main(String[] args, ClassWorld world) throws IOException {
        String mainClass = System.getProperty(MAVEN_MAIN_CLASS_PROPERTY);
        if (mainClass != null && !MavenCling.class.getName().equals(mainClass)) {
            return delegateMain(mainClass, args, world);
        }
        return new MavenCling(world).run(args, null, null, null, false);
    }

    private static int delegateMain(String mainClass, String[] args, ClassWorld world) throws IOException {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(mainClass);
            Method method = clazz.getMethod("main", String[].class, ClassWorld.class);
            return (int) method.invoke(null, args, world);
        } catch (ClassNotFoundException e) {
            throw new IOException("Cannot find maven.mainClass: " + mainClass, e);
        } catch (NoSuchMethodException e) {
            throw new IOException("maven.mainClass does not have main(String[], ClassWorld) method: " + mainClass, e);
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
            throw new IOException("Failed to invoke maven.mainClass: " + mainClass, cause != null ? cause : e);
        } catch (IllegalAccessException e) {
            throw new IOException("Cannot access main method of maven.mainClass: " + mainClass, e);
        }
    }

    /**
     * ClassWorld Launcher "embedded" entry point: returning exitCode and accepts Class World and streams.
     */
    public static int main(
            String[] args,
            ClassWorld world,
            @Nullable InputStream stdIn,
            @Nullable OutputStream stdOut,
            @Nullable OutputStream stdErr)
            throws IOException {
        return new MavenCling(world).run(args, stdIn, stdOut, stdErr, true);
    }

    public MavenCling() {
        super();
    }

    public MavenCling(ClassWorld classWorld) {
        super(classWorld);
    }

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
}
