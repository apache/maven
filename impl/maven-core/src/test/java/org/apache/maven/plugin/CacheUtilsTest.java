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
package org.apache.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CacheUtilsTest {

    private Plugin pluginOne;
    private Plugin pluginTwo;

    @BeforeEach
    void doBeforeEachTest() {
        pluginOne = new Plugin();
        pluginTwo = new Plugin();
    }

    @Test
    void testPluginEqualsEmptyPlugins() {
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginTwo));
    }

    @Test
    void testPluginEqualsSetArtifactId() {
        pluginOne.setArtifactId("myArtifact");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        pluginTwo.setArtifactId("myArtifact");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginTwo));
    }

    @Test
    void testPluginEqualsSetGroupId() {
        pluginOne.setGroupId("myGroupId");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        pluginTwo.setGroupId("myGroupId");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginTwo));
    }

    @Test
    void testPluginEqualsSetVersion() {
        pluginOne.setVersion("myVersion");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        pluginTwo.setVersion("myVersion");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginTwo));
    }

    @Test
    void testPluginEqualsSetExtension() {
        pluginOne.setExtensions("true");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        pluginTwo.setExtensions("TRUE");
    }

    @Test
    void testPluginEqualsSetDependency() {
        Dependency dependencyOne = new Dependency();
        pluginOne.addDependency(dependencyOne);

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        Dependency dependencyTwo = new Dependency();
        pluginTwo.addDependency(dependencyTwo);

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        dependencyTwo.setGroupId("myDependencyGroupId");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        dependencyOne.setGroupId("myDependencyGroupId");
        dependencyTwo.setArtifactId("myDependencyArtifactId");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        dependencyTwo.setVersion("myDependencyVersion");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));

        dependencyOne.setArtifactId("myDependencyArtifactId");
        dependencyTwo.setVersion("myDependencyVersion");

        assertTrue(CacheUtils.pluginEquals(pluginOne, pluginOne));
        assertFalse(CacheUtils.pluginEquals(pluginOne, pluginTwo));
    }
}
