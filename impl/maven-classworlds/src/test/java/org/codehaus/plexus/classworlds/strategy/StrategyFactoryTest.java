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

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrategyFactoryTest {

    private ClassRealm realm;

    @BeforeEach
    void setUp() throws Exception {
        realm = new ClassWorld().newRealm("test");
    }

    @Test
    void defaultStrategyIsSelfFirst() {
        assertInstanceOf(SelfFirstStrategy.class, StrategyFactory.getStrategy(realm));
    }

    @Test
    void selfFirstHint() {
        assertInstanceOf(SelfFirstStrategy.class, StrategyFactory.getStrategy(realm, "self-first"));
    }

    @Test
    void defaultHintIsSelfFirst() {
        assertInstanceOf(SelfFirstStrategy.class, StrategyFactory.getStrategy(realm, "default"));
    }

    @Test
    void nullHintIsSelfFirst() {
        assertInstanceOf(SelfFirstStrategy.class, StrategyFactory.getStrategy(realm, null));
    }

    @Test
    void emptyHintIsSelfFirst() {
        assertInstanceOf(SelfFirstStrategy.class, StrategyFactory.getStrategy(realm, ""));
    }

    @Test
    void parentFirstHint() {
        assertInstanceOf(ParentFirstStrategy.class, StrategyFactory.getStrategy(realm, "parent-first"));
    }

    @Test
    void osgiHint() {
        assertInstanceOf(OsgiBundleStrategy.class, StrategyFactory.getStrategy(realm, "osgi"));
    }

    @Test
    void unknownHintThrows() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> StrategyFactory.getStrategy(realm, "bogus"));
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
