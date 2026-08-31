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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that dispose() does not dispose ClassRealms prematurely.
 * <p>
 * Plexus Disposable.dispose() runs before Sisu's @PreDestroy callbacks.
 * If dispose() disposes ClassRealms, beans loaded from those realms will
 * get ClassNotFoundException when their @PreDestroy methods execute.
 * dispose() should only clear the cache map; flush() should dispose realms.
 *
 * @see <a href="https://github.com/apache/maven/issues/10571">MNG-8572</a>
 */
class DefaultRealmCacheDisposeTest {

    @Test
    void disposeDoesNotDisposeClassRealms() throws Exception {
        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-plugin-realm");

        DefaultPluginRealmCache cache = new DefaultPluginRealmCache();
        PluginRealmCache.CacheRecord record = new PluginRealmCache.CacheRecord(realm, List.<Artifact>of());
        cache.cache.put(new TestKey(), record);

        cache.dispose();

        assertTrue(cache.cache.isEmpty(), "dispose() should clear the cache");
        assertNotNull(world.getClassRealm("test-plugin-realm"), "dispose() should NOT dispose the ClassRealm");
    }

    @Test
    void flushDisposesClassRealms() throws Exception {
        ClassWorld world = new ClassWorld();
        ClassRealm realm = world.newRealm("test-plugin-realm-flush");

        DefaultPluginRealmCache cache = new DefaultPluginRealmCache();
        PluginRealmCache.CacheRecord record = new PluginRealmCache.CacheRecord(realm, List.<Artifact>of());
        cache.cache.put(new TestKey(), record);

        cache.flush();

        assertTrue(cache.cache.isEmpty(), "flush() should clear the cache");
        assertNull(world.getClassRealm("test-plugin-realm-flush"), "flush() SHOULD dispose the ClassRealm");
    }

    private static class TestKey implements PluginRealmCache.Key {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestKey;
        }
    }
}
