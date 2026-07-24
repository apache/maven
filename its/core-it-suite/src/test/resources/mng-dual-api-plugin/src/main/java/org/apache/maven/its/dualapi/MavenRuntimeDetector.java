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
package org.apache.maven.its.dualapi;

import java.lang.reflect.Method;

import org.apache.maven.api.Session;
import org.apache.maven.execution.MavenSession;

/**
 * Bridges from the legacy {@link MavenSession} (Maven 3 API) to the new
 * immutable {@link Session} (Maven 4 API).
 * <p>
 * The ONE place where reflection is unavoidable: Maven 3's
 * {@code MavenSession} does not declare a {@code getSession()} method
 * that returns {@code org.apache.maven.api.Session} — that method was
 * added by Maven 4's compat module. So we call it reflectively,
 * then cast the result to the real typed {@link Session}.
 * <p>
 * <b>Callers must catch {@link NoClassDefFoundError}</b> around any
 * reference to this class, because importing {@link Session} triggers
 * a class-load that fails on Maven 3 (the class doesn't exist).
 */
public final class MavenRuntimeDetector {

    private MavenRuntimeDetector() {}

    /**
     * Obtains the Maven 4 {@link Session} from a legacy
     * {@link MavenSession} via the one-shot reflection bridge.
     *
     * @param legacy the injected Maven 3 session
     * @return the Maven 4 Session, or {@code null} if the bridge
     *         method is absent or returns null
     */
    public static Session getMaven4Session(MavenSession legacy) {
        try {
            Method getSession = legacy.getClass().getMethod("getSession");
            Object result = getSession.invoke(legacy);
            if (result instanceof Session) {
                return (Session) result;
            }
        } catch (ReflectiveOperationException e) {
            // getSession() doesn't exist — shouldn't happen if we got
            // this far (class loading already proved Maven 4 API is
            // present), but handle gracefully.
        }
        return null;
    }
}
