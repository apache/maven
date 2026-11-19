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
package org.apache.maven.its.mng8099;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that implementation-scoped transitive dependencies are available at runtime.
 *
 * <p>Library depends on lib-impl-dep with {@code implementation} scope.
 * The consumer compiles only against Library (lib-impl-dep is not visible at compile time).
 * At test execution, the JVM must be able to load ImplDep (from lib-impl-dep) transitively,
 * because implementation deps are transitive at runtime even though they are not at compile time.
 *
 * <p>If RS_MAIN_COMPILE_PLUS_RUNTIME incorrectly treats implementation as non-transitive,
 * surefire's test classpath will omit lib-impl-dep and this test will fail with NoClassDefFoundError.
 */
public class LibraryRuntimeTest {

    @Test
    public void testImplementationDepIsAvailableAtRuntime() {
        // Library.getImplMessage() internally calls new ImplDep().getMessage().
        // ImplDep is an implementation-scoped transitive dep — not visible at compile time
        // in this consumer, but must be on the runtime classpath for this call to succeed.
        Library library = new Library();
        String message = library.getImplMessage();
        Assert.assertEquals("Implementation dependency", message);
    }
}
