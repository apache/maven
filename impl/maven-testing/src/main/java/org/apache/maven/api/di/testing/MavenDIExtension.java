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
package org.apache.maven.api.di.testing;

import java.io.File;

import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit Jupiter extension that provides dependency injection support for Maven tests.
 * This extension manages the lifecycle of a DI container for each test method execution,
 * automatically performing injection into test instances and cleanup.
 *
 * <p>This is a modernized version of the original Plexus test support, adapted for
 * Maven's new DI framework and JUnit Jupiter.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @ExtendWith(MavenDIExtension.class)
 * class MyTest {
 *     @Inject
 *     private MyComponent component;
 *
 *     @Test
 *     void testSomething() {
 *         // component is automatically injected
 *     }
 * }
 * }
 * </pre>
 */
public class MavenDIExtension implements BeforeEachCallback, AfterEachCallback {
    protected static ExtensionContext context;
    protected Injector injector;
    protected static String basedir;

    /**
     * Initializes the test environment before each test method execution.
     * Sets up the base directory and DI container, then performs injection into the test instance.
     *
     * @param context The extension context provided by JUnit
     * @throws Exception if initialization fails
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        basedir = getBasedir();
        setContext(context);
        getInjector().bindInstance((Class<Object>) context.getRequiredTestClass(), context.getRequiredTestInstance());
        getInjector().injectInstance(context.getRequiredTestInstance());
    }

    /**
     * Stores the extension context for use during test execution.
     *
     * @param context The extension context to store
     */
    protected void setContext(ExtensionContext context) {
        MavenDIExtension.context = context;
    }

    /**
     * Creates and configures the DI container for test execution.
     * Performs component discovery and sets up basic bindings.
     *
     * @throws IllegalArgumentException if container setup fails
     */
    @SuppressWarnings("unchecked")
    protected void setupContainer() {
        try {
            injector = Injector.create();
            injector.bindInstance(ExtensionContext.class, context);
            injector.discover(context.getRequiredTestClass().getClassLoader());
            injector.bindInstance(Injector.class, injector);
            injector.bindInstance((Class) context.getRequiredTestClass(), context.getRequiredTestInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create DI injector.", e);
        }
    }

    /**
     * Cleans up resources after each test method execution.
     * Currently a placeholder for future cleanup implementation.
     *
     * @param context The extension context provided by JUnit
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (injector != null) {
            // TODO: implement
            // injector.dispose();
            injector = null;
        }
    }

    /**
     * Returns the DI injector, creating it if necessary.
     *
     * @return The configured injector instance
     */
    public Injector getInjector() {
        if (injector == null) {
            setupContainer();
        }
        return injector;
    }

    /**
     * Looks up a component of the specified type from the container.
     *
     * @param <T> The component type
     * @param componentClass The class of the component to look up
     * @return The component instance
     * @throws DIException if lookup fails
     */
    protected <T> T lookup(Class<T> componentClass) throws DIException {
        return getInjector().getInstance(componentClass);
    }

    /**
     * Looks up a component of the specified type and role hint from the container.
     *
     * @param <T> The component type
     * @param componentClass The class of the component to look up
     * @param roleHint The role hint for the component
     * @return The component instance
     * @throws DIException if lookup fails
     */
    protected <T> T lookup(Class<T> componentClass, String roleHint) throws DIException {
        return getInjector().getInstance(Key.ofType(componentClass, roleHint));
    }

    /**
     * Looks up a component of the specified type and qualifier from the container.
     *
     * @param <T> The component type
     * @param componentClass The class of the component to look up
     * @param qualifier The qualifier for the component
     * @return The component instance
     * @throws DIException if lookup fails
     */
    protected <T> T lookup(Class<T> componentClass, Object qualifier) throws DIException {
        return getInjector().getInstance(Key.ofType(componentClass, qualifier));
    }

    /**
     * Releases a component back to the container.
     * Currently a placeholder for future implementation.
     *
     * @param component The component to release
     * @throws DIException if release fails
     */
    protected void release(Object component) throws DIException {
        // TODO: implement
        // getInjector().release(component);
    }

    /**
     * Creates a File object for a path relative to the base directory.
     *
     * @param path The relative path
     * @return A File object representing the path
     */
    public static File getTestFile(String path) {
        return new File(getBasedir(), path);
    }

    /**
     * Creates a File object for a path relative to a specified base directory.
     *
     * @param basedir The base directory path
     * @param path The relative path
     * @return A File object representing the path
     */
    public static File getTestFile(String basedir, String path) {
        File basedirFile = new File(basedir);

        if (!basedirFile.isAbsolute()) {
            basedirFile = getTestFile(basedir);
        }

        return new File(basedirFile, path);
    }

    /**
     * Returns the absolute path for a path relative to the base directory.
     *
     * @param path The relative path
     * @return The absolute path
     */
    public static String getTestPath(String path) {
        return getTestFile(path).getAbsolutePath();
    }

    /**
     * Returns the absolute path for a path relative to a specified base directory.
     *
     * @param basedir The base directory path
     * @param path The relative path
     * @return The absolute path
     */
    public static String getTestPath(String basedir, String path) {
        return getTestFile(basedir, path).getAbsolutePath();
    }

    /**
     * Returns the base directory for test execution.
     * Uses the "basedir" system property if set, otherwise uses the current directory.
     *
     * @return The base directory path
     */
    public static String getBasedir() {
        if (basedir != null) {
            return basedir;
        }

        basedir = System.getProperty("basedir");

        if (basedir == null) {
            basedir = new File("").getAbsolutePath();
        }

        return basedir;
    }
}
