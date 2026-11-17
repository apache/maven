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
package org.codehaus.plexus.classworlds.strategy;

/*
 * Copyright 2001-2006 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// jars within jars
// hierarchy vs graph

class StrategyTest extends AbstractClassWorldsTestCase {
    private ClassRealm realm;

    private Strategy strategy;

    @BeforeEach
    public void setUp() throws Exception {
        this.realm = new ClassWorld().newRealm("realm");
        this.strategy = this.realm.getStrategy();
        realm.addURL(getJarUrl("component0-1.0.jar"));
    }

    @Test
    void testLoadingOfApplicationClass() throws Exception {
        assertNotNull(strategy.loadClass("org.codehaus.plexus.Component0"));
    }

    @Test
    void testLoadingOfApplicationClassThenDoingItAgain() throws Exception {
        Class<?> c = strategy.loadClass("org.codehaus.plexus.Component0");

        assertNotNull(c);

        c = strategy.loadClass("org.codehaus.plexus.Component0");

        assertNotNull(c);
    }

    @Test
    void testLoadingOfSystemClass() throws Exception {
        assertNotNull(strategy.getRealm().loadClass("java.lang.Object"));
    }

    @Test
    void testLoadingOfNonExistentClass() {
        try {
            strategy.loadClass("org.codehaus.plexus.NonExistentComponent");

            fail("Should have thrown a ClassNotFoundException!");
        } catch (ClassNotFoundException e) {
            // do nothing
        }
    }

    @Test
    void testGetApplicationResource() throws Exception {
        URL resource = strategy.getResource("META-INF/plexus/components.xml");

        assertNotNull(resource);

        String content = getContent(resource.openStream());

        assertTrue(content.startsWith("<component-set>"));
    }

    @Test
    void testGetSystemResource() {
        assumeTrue(
                getJavaVersion() < 9.0,
                "Due to strong encapsulation you cannot get the java/lang/Object.class as resource since Java 9");

        URL resource = strategy.getRealm().getResource("java/lang/Object.class");

        assertNotNull(resource);
    }

    @Test
    void testFindResources() throws Exception {
        realm.addURL(getJarUrl("component1-1.0.jar"));

        Enumeration<URL> e = strategy.getResources("META-INF/plexus/components.xml");
        assertNotNull(e);

        int resourceCount = 0;
        while (e.hasMoreElements()) {
            e.nextElement();
            resourceCount++;
        }
        assertEquals(2, resourceCount);
    }

    protected String getContent(InputStream in) throws Exception {
        byte[] buffer = new byte[1024];

        int read;

        StringBuilder content = new StringBuilder();

        while ((read = in.read(buffer, 0, 1024)) >= 0) {
            content.append(new String(buffer, 0, read));
        }

        return content.toString();
    }

    private double getJavaVersion() {
        return Double.parseDouble(System.getProperty("java.specification.version"));
    }
}
