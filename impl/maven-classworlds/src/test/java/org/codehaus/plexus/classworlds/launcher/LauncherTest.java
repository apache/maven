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
package org.codehaus.plexus.classworlds.launcher;

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
import java.io.File;
import java.io.FileInputStream;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class LauncherTest extends AbstractClassWorldsTestCase {
    private Launcher launcher;

    @BeforeEach
    public void setUp() {
        this.launcher = new Launcher();

        this.launcher.setSystemClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @AfterEach
    public void tearDown() {
        this.launcher = null;
    }

    @Test
    void testConfigureValid() throws Exception {
        launcher.configure(getConfigPath("valid-launch.conf"));

        Class<?> mainClass = launcher.getMainClass();

        assertNotNull(mainClass);

        assertEquals("a.A", mainClass.getName());

        assertEquals("app", launcher.getMainRealm().getId());
    }

    @Test
    void testLaunchValidStandard() throws Exception {
        launcher.configure(getConfigPath("valid-launch.conf"));

        launcher.launch(new String[] {});
    }

    @Test
    void testLaunchValidStandardExitCode() throws Exception {
        launcher.configure(getConfigPath("valid-launch-exitCode.conf"));

        launcher.launch(new String[] {});

        assertEquals(15, launcher.getExitCode(), "check exit code");
    }

    @Test
    void testLaunchValidEnhanced() throws Exception {
        launcher.configure(getConfigPath("valid-enh-launch.conf"));

        launcher.launch(new String[] {});
    }

    @Test
    void testLaunchValidEnhancedExitCode() throws Exception {
        launcher.configure(getConfigPath("valid-enh-launch-exitCode.conf"));

        launcher.launch(new String[] {});

        assertEquals(45, launcher.getExitCode(), "check exit code");
    }

    @Test
    void testLaunchNoSuchMethod() throws Exception {
        launcher.configure(getConfigPath("launch-nomethod.conf"));

        try {
            launcher.launch(new String[] {});
            fail("should have thrown NoSuchMethodException");
        } catch (NoSuchMethodException e) {
            // expected and correct
        }
    }

    @Test
    void testLaunchClassNotFound() throws Exception {
        launcher.configure(getConfigPath("launch-noclass.conf"));

        try {
            launcher.launch(new String[] {});
            fail("throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }
    }

    private FileInputStream getConfigPath(String name) throws Exception {
        String basedir = TestUtil.getBasedir();

        return new FileInputStream(new File(new File(basedir, "src/test/test-data"), name));
    }
}
