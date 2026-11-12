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

import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * StrategyFactory loads a strategy, either default or from a given hint.
 *
 * <p>Supported hints:
 * <ul>
 *   <li>{@code "self-first"} (default) — realm's own classpath is searched before the parent</li>
 *   <li>{@code "parent-first"} — parent classloader is searched before the realm's own classpath</li>
 *   <li>{@code "osgi"} — OSGi bundle loading order (java.*, imports, self, fragments)</li>
 * </ul>
 */
public class StrategyFactory {

    public static Strategy getStrategy(ClassRealm realm) {
        return getStrategy(realm, "self-first");
    }

    public static Strategy getStrategy(ClassRealm realm, String hint) {
        if (hint == null || hint.isEmpty() || "self-first".equals(hint) || "default".equals(hint)) {
            return new SelfFirstStrategy(realm);
        } else if ("parent-first".equals(hint)) {
            return new ParentFirstStrategy(realm);
        } else if ("osgi".equals(hint)) {
            return new OsgiBundleStrategy(realm);
        } else {
            throw new IllegalArgumentException(
                    "Unknown strategy hint '" + hint + "': supported hints are 'self-first', 'parent-first', 'osgi'");
        }
    }
}
