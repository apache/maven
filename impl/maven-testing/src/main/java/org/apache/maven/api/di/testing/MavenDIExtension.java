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

import java.io.*;

import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This is a slightly modified version of the original plexus class
 * available at https://raw.githubusercontent.com/codehaus-plexus/plexus-containers/master/plexus-container-default/
 *              src/main/java/org/codehaus/plexus/PlexusTestCase.java
 * in order to migrate the tests to JUnit 4.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @author Guillaume Nodet
 */
public class MavenDIExtension implements BeforeEachCallback, AfterEachCallback {
    protected static ExtensionContext context;
    protected Injector injector;
    protected static String basedir;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        basedir = getBasedir();

        setContext(context);

        getInjector().bindInstance((Class<Object>) context.getRequiredTestClass(), context.getRequiredTestInstance());
        getInjector().injectInstance(context.getRequiredTestInstance());
    }

    protected void setContext(ExtensionContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    protected void setupContainer() {
        try {
            injector = Injector.create();
            injector.bindInstance(ExtensionContext.class, this.context);
            injector.discover(this.context.getRequiredTestClass().getClassLoader());
            injector.bindInstance(Injector.class, injector);
            injector.bindInstance((Class) this.context.getRequiredTestClass(), this.context.getRequiredTestInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create DI injector.", e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (injector != null) {
            // TODO: implement
            // injector.dispose();
            injector = null;
        }
    }

    public Injector getInjector() {
        if (injector == null) {
            setupContainer();
        }

        return injector;
    }

    // ----------------------------------------------------------------------
    // Container access
    // ----------------------------------------------------------------------

    protected <T> T lookup(Class<T> componentClass) throws DIException {
        return getInjector().getInstance(componentClass);
    }

    protected <T> T lookup(Class<T> componentClass, String roleHint) throws DIException {
        return getInjector().getInstance(Key.ofType(componentClass, roleHint));
    }

    protected <T> T lookup(Class<T> componentClass, Object qualifier) throws DIException {
        return getInjector().getInstance(Key.ofType(componentClass, qualifier));
    }

    protected void release(Object component) throws DIException {
        // TODO: implement
        // getInjector().release(component);
    }

    // ----------------------------------------------------------------------
    // Helper methods for sub classes
    // ----------------------------------------------------------------------

    public static File getTestFile(String path) {
        return new File(getBasedir(), path);
    }

    public static File getTestFile(String basedir, String path) {
        File basedirFile = new File(basedir);

        if (!basedirFile.isAbsolute()) {
            basedirFile = getTestFile(basedir);
        }

        return new File(basedirFile, path);
    }

    public static String getTestPath(String path) {
        return getTestFile(path).getAbsolutePath();
    }

    public static String getTestPath(String basedir, String path) {
        return getTestFile(basedir, path).getAbsolutePath();
    }

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
